package nl.hbelectronics.hpgastest;

import android.Manifest;
import android.app.*;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.security.SecureRandom;

/** Gas Alert v3.0 prototype BLE transport. Owns reconnect, never changes mode on reconnect. */
public final class HpBleService extends Service {
 public static final UUID SERVICE=UUID.fromString("b8f23000-6a9b-4a57-9b80-43db7c4f1000");
 private static final UUID CMD=UUID.fromString("b8f23001-6a9b-4a57-9b80-43db7c4f1000");
 private static final UUID STATUS=UUID.fromString("b8f23002-6a9b-4a57-9b80-43db7c4f1000");
 private static final UUID CONFIG=UUID.fromString("b8f23003-6a9b-4a57-9b80-43db7c4f1000");
 private static final UUID CCC=UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
 public static final String ACTION_CONNECT="nl.hbelectronics.gasalertv50.CONNECT";
 public static final String ACTION_DISCONNECT="nl.hbelectronics.gasalertv50.DISCONNECT";
 public static final String EXTRA_ADDRESS="address";
 public static final String EXTRA_NAME="name";
 private static final String PREFS="gas_alert_v50", CHANNEL="gas_alert_v50_ble";
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final IBinder binder=new LocalBinder();
 public final class LocalBinder extends Binder { public HpBleService getService(){return HpBleService.this;} }
 public interface Listener { void changed(); }
 private Listener listener;
 private BluetoothManager manager;
 private BluetoothAdapter adapter;
 private BluetoothGatt gatt;
 private BluetoothLeScanner scanner;
 private boolean scanning=false,active=false,initialRead=false,writing=false,reading=false,authorized=false,subscribing=false,subscribed=false;
 private BluetoothGattCharacteristic commandCharacteristic, statusCharacteristic, configCharacteristic;
 private final ArrayDeque<String> sendQueue=new ArrayDeque<>();
 private long lastStatusMs=0;
 private int signalDbm=0;
 private long lastRssi=0,lastRssiRequest=0;
 private boolean rssiPending=false;
 private String ownerToken="";
 private boolean authorizing=false,releasePending=false;
 private String writingCommand="";
 private final Runnable releaseTimeout=()->{
  if(releasePending){
   releasePending=false;
   message="Release not confirmed. Controller ownership may remain reserved. Reconnect and retry.";
   signal();
  }
 };
 // Manual release has priority and must be acknowledged by the ESP32 before closing GATT.
 private final Runnable releaseProbe=new Runnable(){
  @Override public void run(){
   if(!releasePending||gatt==null)return;
   if(ready&&!writing&&!reading&&!authorizing&&!subscribing){
    if(!sendQueue.isEmpty())drain();
    else readStatus();
   }
   handler.postDelayed(this,400);
  }
 };
 public interface SaveCallback { void completed(boolean confirmed,String detail); }
 private SaveCallback saveCallback;
 private String saveKey="",saveValue="",saveCommand="";
 private boolean saveAwaitingRead=false;
 private final Runnable saveTimeout=()->finishSave(false,"No confirmation received from controller.");
 private void finishSave(boolean confirmed,String detail){
  if(saveCallback==null)return;
  SaveCallback cb=saveCallback;
  saveCallback=null;saveKey="";saveValue="";saveCommand="";saveAwaitingRead=false;
  handler.removeCallbacks(saveTimeout);
  cb.completed(confirmed,detail);
 }
 private String configValue(String cfg,String key){
  if(cfg==null)return null;
  for(String item:cfg.split(";")){
   String[] p=item.split("=",2);
   if(p.length==2&&key.equals(p[0]))return p[1];
  }
  return null;
 }
 public boolean saveSetting(String key,String value,SaveCallback cb){
  if(cb==null||key==null||value==null||saveCallback!=null||releasePending||!ready||!authorized||!"NORMAL".equals(mode))return false;
  // No concurrent queued settings/preview commands during verified readback.
  sendQueue.removeIf(q->q.startsWith("PREVIEW "));
  saveCallback=cb;saveKey=key;saveValue=value;saveCommand="SET "+key+"="+value;
  handler.postDelayed(saveTimeout,8500);
  sendQueue.offer(saveCommand);
  drain();
  return true;
 }
 public String address="",name="",mode="UNKNOWN",message="Not connected",config="";
 public String lastDisconnectReason="";
 public long revision=-1;
 public boolean ready=false, busy=false;
 // v1.4 lifecycle: timeout for connection setup, then bounded scans and retries.
 private static final long RETRY_MS=5000L, SCAN_DURATION_MS=9000L, CONNECT_TIMEOUT_MS=16000L;
 private final Runnable connectTimeout=()->{
  if(active&&!ready&&gatt!=null)lost("Connection setup timed out — retrying");
 };
 private final BroadcastReceiver bluetoothState=new BroadcastReceiver(){
  @Override public void onReceive(Context context,Intent intent){
   int state=intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,-1);
   if(state==BluetoothAdapter.STATE_OFF&&active)lost("Bluetooth OFF — awaiting adapter");
   else if(state==BluetoothAdapter.STATE_ON&&active&&!ready)scheduleRetry();
  }
 };
 private boolean receiverRegistered=false;
 private final Runnable retry=()->{if(active&&!ready)scanForOwner();};
 private final Runnable retryScanEnd=()->{stopScan();if(active&&!ready)scheduleRetry();};
 private final Runnable statusPoll=new Runnable(){
  @Override public void run(){
   if(active&&ready&&!releasePending&&gatt!=null&&!writing&&!reading&&!authorizing&&!subscribing){
    long now=SystemClock.elapsedRealtime();
    // v1.4 pattern: read RSSI regularly; never overlap a characteristic operation.
    if(now-lastRssiRequest>=3500 && (!rssiPending||now-lastRssiRequest>6500)){
     requestRssi();
    }else if(!rssiPending&&now-lastStatusMs>=6500){
     readStatus();
    }
   }
   handler.postDelayed(this,700);
  }
 };
 private void requestRssi(){
  if(gatt==null||!ready||!permitted()||reading||writing||authorizing||subscribing)return;
  lastRssiRequest=SystemClock.elapsedRealtime();
  try{
   rssiPending=gatt.readRemoteRssi();
   if(!rssiPending)android.util.Log.w("GasAlertBLE","RSSI request rejected");
  }catch(Exception e){
   rssiPending=false;
   android.util.Log.w("GasAlertBLE","RSSI request failed",e);
  }
 }
 private final Runnable watchdog=new Runnable(){
  @Override public void run(){
   if(active&&ready&&SystemClock.elapsedRealtime()-lastStatusMs>30000){
    // Do not tear down a valid BLE link because an app-level status read is delayed.
    message="Bluetooth connected · waiting for fresh controller status";signal();
   }
   handler.postDelayed(this,3000);
  }
 };
 @Override public void onCreate(){
  super.onCreate();
  manager=getSystemService(BluetoothManager.class);
  adapter=manager==null?null:manager.getAdapter();
  android.content.SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);
  address=p.getString("address","");name=p.getString("name","");active=!address.isEmpty();
  ownerToken=p.getString("owner_token","");
  if(ownerToken.length()!=32){
   byte[] bytes=new byte[16];new SecureRandom().nextBytes(bytes);
   StringBuilder hex=new StringBuilder();
   for(byte b:bytes)hex.append(String.format(Locale.ROOT,"%02x",b&0xff));
   ownerToken=hex.toString();
   p.edit().putString("owner_token",ownerToken).apply();
  }
  registerReceiver(bluetoothState,new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
  receiverRegistered=true;
  NotificationManager nm=getSystemService(NotificationManager.class);
  nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Gas Alert v5.0 Bluetooth",NotificationManager.IMPORTANCE_LOW));
  handler.post(watchdog);
  handler.postDelayed(statusPoll,1000);
  if(active)handler.post(()->{if(active){foreground();connect();}});

 }
 @Override public IBinder onBind(Intent i){return binder;}
 public void setListener(Listener l){listener=l;signal();}
 private void signal(){if(listener!=null)handler.post(()->{if(listener!=null)listener.changed();});}
 private Notification notification(){
  Intent open=new Intent(this,MainActivity.class);
  PendingIntent intent=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  return new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
    .setContentTitle("HP Gas Alert System v5.0")
    .setContentText(ready?"Bluetooth connected":active?"Bluetooth reconnecting":"Bluetooth disconnected")
    .setContentIntent(intent).setOngoing(active).build();
 }
 private void foreground(){
  try{startForeground(41,notification());}catch(Exception e){message="Foreground notification error: "+e.getMessage();signal();}
 }
 @Override public int onStartCommand(Intent i,int flags,int startId){
  if(i!=null&&ACTION_CONNECT.equals(i.getAction())){
   String a=i.getStringExtra(EXTRA_ADDRESS),n=i.getStringExtra(EXTRA_NAME);
   if(a!=null&&!a.isEmpty()){
    // Explicit Connect/Change Device must replace any previous GATT session.
    stopScan();handler.removeCallbacks(retry);closeGatt();
    ready=false;releasePending=false;
    address=a;name=n==null?"HP Gas Alert System v5.0":n;active=true;
    getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString("address",address).putString("name",name).apply();
    foreground();connect();
   }
  }else if(i!=null&&ACTION_DISCONNECT.equals(i.getAction())){
   finishSave(false,"Controller is disconnecting.");
   requestOwnerRelease();
  }else if(active){foreground();connect();}
  return START_STICKY;
 }
 private void requestOwnerRelease(){
  if(releasePending)return;
  if(!active||!ready||!authorized||gatt==null){
   message="Owner cannot be released offline. Reconnect to the controller first.";
   signal();return;
  }
  releasePending=true;
  sendQueue.clear();
  sendQueue.offer("RELEASE");
  // Do not start background polling while RELEASE is in flight.
  message="Releasing controller ownership…";
  signal();
  handler.removeCallbacks(releaseTimeout);
  handler.removeCallbacks(releaseProbe);
  handler.postDelayed(releaseTimeout,10000);
  drain();
  handler.postDelayed(releaseProbe,400);
 }
 private void finishManualDisconnect(){
  if(!releasePending)return;
  releasePending=false;handler.removeCallbacks(releaseTimeout);handler.removeCallbacks(releaseProbe);
  active=false;address="";name="";
  getSharedPreferences(PREFS,MODE_PRIVATE).edit().remove("address").remove("name").apply();
  stopScan();handler.removeCallbacks(retry);handler.removeCallbacks(retryScanEnd);
  closeGatt();
  ready=false;authorized=false;
  mode="UNKNOWN";revision=-1;
  message="Disconnected — ownership released";
  signal();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
 }
 public boolean hasSelection(){return active&&!address.isEmpty();}
 public String signalLabel(){
  if(!ready)return "Signal: unavailable (disconnected)";
  if(lastRssi==0)return "Signal: measuring…";
  long age=SystemClock.elapsedRealtime()-lastRssi;
  return "Signal: "+signalDbm+" dBm"+(age>12000?" (last measurement)":"");
 }
 private boolean permitted(){return checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED
  &&checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)==PackageManager.PERMISSION_GRANTED;}
 private void connect(){
  if(!active||ready||gatt!=null)return;
  if(!permitted()||adapter==null||!adapter.isEnabled()){message="Bluetooth unavailable or permission missing";signal();scheduleRetry();return;}
  stopScan();handler.removeCallbacks(retry);
  ready=false;initialRead=false;authorized=false;authorizing=false;subscribing=false;subscribed=false;mode="UNKNOWN";revision=-1;config="";sendQueue.clear();writing=false;reading=false;busy=false;lastRssi=0;lastRssiRequest=0;signalDbm=0;rssiPending=false;
  message="Connecting to "+name+"…";signal();
  try{
   BluetoothDevice device=adapter.getRemoteDevice(address);
   gatt=device.connectGatt(this,false,callbacks,BluetoothDevice.TRANSPORT_LE);
   if(gatt==null){lost("Cannot initiate BLE link");return;}
   handler.removeCallbacks(connectTimeout);
   handler.postDelayed(connectTimeout,CONNECT_TIMEOUT_MS);
  }catch(Exception e){message="Connection error: "+e.getMessage();signal();scheduleRetry();}
 }
 private void scheduleRetry(){
  if(!active)return;
  handler.removeCallbacks(retry);handler.postDelayed(retry,RETRY_MS);
 }
 private void lost(String reason){lost();message=reason;signal();}
 private void lost(){
  finishSave(false,"Bluetooth connection interrupted before confirmation.");
  ready=false;initialRead=false;authorized=false;authorizing=false;subscribing=false;subscribed=false;mode="UNKNOWN";revision=-1;busy=false;writing=false;reading=false;sendQueue.clear();lastRssi=0;lastRssiRequest=0;signalDbm=0;rssiPending=false;
  if(releasePending){
   releasePending=false;
   handler.removeCallbacks(releaseTimeout);
   handler.removeCallbacks(releaseProbe);
   message="Link lost before release confirmation; reconnecting to check ownership";
  }else message=active?"Bluetooth link disconnected — automatic reconnect":"Disconnected";
  signal();foreground();
  closeGatt();if(active)scheduleRetry();
 }
 private void closeGatt(){
  handler.removeCallbacks(connectTimeout);
  BluetoothGatt old=gatt;gatt=null;
  if(old!=null&&permitted())try{old.disconnect();old.close();}catch(Exception ignored){}
 }
 private void scanForOwner(){
  if(!active||scanning||!permitted()||adapter==null||!adapter.isEnabled()){scheduleRetry();return;}
  scanner=adapter.getBluetoothLeScanner();
  if(scanner==null){scheduleRetry();return;}
  scanning=true;message="Searching for reserved controller…";signal();
  try{
   List<ScanFilter> filters=Collections.singletonList(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(SERVICE)).build());
   scanner.startScan(filters,new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),scanCallback);
   handler.postDelayed(retryScanEnd,SCAN_DURATION_MS);
  }catch(Exception e){scanning=false;scheduleRetry();}
 }
 private final ScanCallback scanCallback=new ScanCallback(){
  @Override public void onScanResult(int callbackType,ScanResult result){
   if(!active||result==null||result.getDevice()==null)return;
   // Firmware advertises our custom v3.0 UUID. Match service, not the rotating
   // BLE address: the previously stored MAC may differ after a reconnect.
   handler.post(()->{
    if(active&&scanning&&!ready&&gatt==null){
     // v1.4: connect only to the controller explicitly selected by the user.
     // Never claim a different unit merely because its advertised UUID matches.
     if(!address.equalsIgnoreCase(result.getDevice().getAddress()))return;
     stopScan();connect();
    }
   });
  }
  @Override public void onScanFailed(int errorCode){handler.post(()->{stopScan();scheduleRetry();});}
 };
 private void stopScan(){
  handler.removeCallbacks(retryScanEnd);
  if(scanning&&scanner!=null&&permitted())try{scanner.stopScan(scanCallback);}catch(Exception ignored){}
  scanning=false;
 }
 private void updateStatus(byte[] data){
  if(data==null)return;
  String value=new String(data,StandardCharsets.UTF_8).trim();
  String[] parts=value.split("\\|",2);
  if(parts.length!=2)return;
  if("RELEASED".equals(parts[0])){
   finishManualDisconnect();return;
  }
  if("LOCKED".equals(parts[0])){
   ready=false;authorized=false;mode="UNKNOWN";revision=-1;
   message="Old owner still reserved — automatic retry (up to 30 min)";signal();return;
  }
  if(!("NORMAL".equals(parts[0])||"ALARM".equals(parts[0])||"OFF".equals(parts[0])))return;
  try{
   long next=Long.parseLong(parts[1].trim());
   mode=parts[0];revision=next;authorized=true;
   lastStatusMs=SystemClock.elapsedRealtime();
   message="Bluetooth connected · controller confirmed";signal();
  }
  catch(NumberFormatException ignored){}
 }
 private void setupCharacteristics(){
  if(gatt==null)return;
  BluetoothGattService svc=gatt.getService(SERVICE);
  if(svc==null){message="Gas Alert BLE service not found";lost();return;}
  commandCharacteristic=svc.getCharacteristic(CMD);
  statusCharacteristic=svc.getCharacteristic(STATUS);
  configCharacteristic=svc.getCharacteristic(CONFIG);
  if(commandCharacteristic==null||statusCharacteristic==null||configCharacteristic==null){message="Incomplete Gas Alert firmware protocol";lost();return;}
  if(!permitted())return;
  // Do not wait for CCC descriptor callbacks: some phones never report them.
  // AUTH must be the first serialized GATT operation after service discovery.
  message="BLE service found - authorizing phone";signal();
  sendAuthorization();
 }
 private void sendAuthorization(){
  if(gatt==null||commandCharacteristic==null||!permitted()||authorizing)return;
  authorizing=true;
  String request="AUTH "+ownerToken;
  int rc=gatt.writeCharacteristic(commandCharacteristic,request.getBytes(StandardCharsets.UTF_8),BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
  if(rc!=BluetoothStatusCodes.SUCCESS){
   authorizing=false;message="BLE authorization write error ("+rc+")";signal();lost();
  }
 }
 private void subscribeStatus(){
  if(gatt==null||statusCharacteristic==null||!permitted()||subscribing||subscribed)return;
  BluetoothGattDescriptor descriptor=statusCharacteristic.getDescriptor(CCC);
  if(descriptor==null||!gatt.setCharacteristicNotification(statusCharacteristic,true)){
   message="Notification unavailable - status polling fallback";signal();
   subscribed=true;readConfig();return;
  }
  subscribing=true;
  int result=gatt.writeDescriptor(descriptor,BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
  if(result!=BluetoothStatusCodes.SUCCESS){
   subscribing=false;subscribed=true;
   message="Notification subscription unsupported - polling fallback";signal();readConfig();
  }
 }
 private void readStatus(){
  if(gatt==null||statusCharacteristic==null||!permitted()||writing||reading||authorizing||subscribing)return;
  reading=gatt.readCharacteristic(statusCharacteristic);
 }

 private void readConfig(){
  if(gatt==null||configCharacteristic==null||!permitted()||writing||reading||authorizing||subscribing)return;
  reading=gatt.readCharacteristic(configCharacteristic);
 }

 private final BluetoothGattCallback callbacks=new BluetoothGattCallback(){
  @Override public void onConnectionStateChange(BluetoothGatt source,int status,int newState){
   handler.post(()->{
    if(source!=gatt)return;
    if(status==BluetoothGatt.GATT_SUCCESS&&newState==BluetoothProfile.STATE_CONNECTED){
     lastDisconnectReason="";
     message="Reading controller…";signal();
     if(!permitted()||!source.discoverServices())lost("BLE service discovery failed");
    }else if(newState==BluetoothProfile.STATE_DISCONNECTED||status!=BluetoothGatt.GATT_SUCCESS){
     lastDisconnectReason="GATT status="+status+", state="+newState;
     android.util.Log.w("GasAlertBLE","GATT disconnected: "+lastDisconnectReason);
     lost();
     message="Bluetooth disconnected ("+lastDisconnectReason+") — reconnecting";signal();
    }
   });
  }
  @Override public void onServicesDiscovered(BluetoothGatt source,int status){
   handler.post(()->{if(source==gatt){if(status==BluetoothGatt.GATT_SUCCESS)setupCharacteristics();else lost();}});
  }
  @Override public void onDescriptorWrite(BluetoothGatt source,BluetoothGattDescriptor d,int status){
   handler.post(()->{
    if(source!=gatt)return;
    subscribing=false;subscribed=true;
    if(status!=BluetoothGatt.GATT_SUCCESS)message="Notifications unavailable - using polling";
    readConfig();
   });
  }
  @Override public void onCharacteristicRead(BluetoothGatt source,BluetoothGattCharacteristic c,byte[] value,int status){
   handler.post(()->{
    if(source!=gatt)return;
    reading=false;
    if(status==BluetoothGatt.GATT_SUCCESS){
     if(c.getUuid().equals(STATUS)){
      updateStatus(value);
      if(source!=gatt)return;
      if(authorized){
       if(!subscribed&&!subscribing)subscribeStatus();
       else if(!ready)readConfig();
      }else if(!releasePending){
       handler.postDelayed(()->{if(source==gatt&&!ready)readStatus();},900);
      }
     }
     else if(c.getUuid().equals(CONFIG)){
      config=new String(value,StandardCharsets.UTF_8);
      ready=authorized&&revision>=0;
      if(ready)handler.removeCallbacks(connectTimeout);
      if(saveAwaitingRead&&saveCallback!=null){
       String actual=configValue(config,saveKey);
       finishSave(saveValue.equals(actual),
         saveValue.equals(actual)?"Controller returned the saved setting.":"Saved value was not confirmed by controller.");
      }
      signal();foreground();drain();
     }
    }else {
     if(saveAwaitingRead)finishSave(false,"Failed to read the setting back from controller.");
     message="BLE read error "+status;signal();
    }
    drain();
   });
  }
  @Override public void onCharacteristicChanged(BluetoothGatt source,BluetoothGattCharacteristic c,byte[] value){
   if(c.getUuid().equals(STATUS))handler.post(()->{
    if(source==gatt){
     updateStatus(value);
     if(source==gatt&&authorized&&!ready&&!subscribing&&subscribed)readConfig();
    }
   });
  }
  @Override public void onReadRemoteRssi(BluetoothGatt source,int rssi,int status){
   handler.post(()->{
    if(source!=gatt||!ready||!active)return;
    rssiPending=false;
    if(status==BluetoothGatt.GATT_SUCCESS&&rssi<0&&rssi>=-127){
     signalDbm=rssi;lastRssi=SystemClock.elapsedRealtime();signal();
    }
   });
  }
  @Override public void onCharacteristicWrite(BluetoothGatt source,BluetoothGattCharacteristic c,int status){
   handler.post(()->{
    if(source!=gatt)return;
    if(authorizing){
     authorizing=false;
     if(status==BluetoothGatt.GATT_SUCCESS){
      message="Authorization sent - reading controller";signal();
      handler.postDelayed(()->{if(source==gatt)readStatus();},650);
     }
     else {message="BLE authorization failed ("+status+")";signal();lost();}
     return;
    }
    String finished=writingCommand;
    writingCommand="";
    writing=false;busy=false;
    if(saveCallback!=null&&finished.equals(saveCommand)){
     if(status!=BluetoothGatt.GATT_SUCCESS){
      finishSave(false,"BLE write failed ("+status+").");
     }else{
      saveAwaitingRead=true;
      // Read back the controller's configuration after its SET command completed.
      handler.postDelayed(()->{
       if(source!=gatt||!saveAwaitingRead)return;
       if(!reading&&!writing&&!subscribing)readConfig();
      },240);
     }
     signal();
     return;
    }
    if("RELEASE".equals(finished)&&status!=BluetoothGatt.GATT_SUCCESS){
     handler.removeCallbacks(releaseTimeout);
     handler.removeCallbacks(releaseProbe);
     releasePending=false;
     message="Owner release failed (BLE "+status+"); ownership retained";
     signal();return;
    }
    if(status!=BluetoothGatt.GATT_SUCCESS)message="Command write failed: "+status;
    signal();
    if(releasePending){
     handler.postDelayed(()->{if(source==gatt)readStatus();},180);
    }else if(!sendQueue.isEmpty())drain();
    else handler.postDelayed(()->{if(source==gatt)readStatus();},180);
   });
  }
 };
 public boolean command(String cmd){
  if(!ready||revision<0||saveCallback!=null)return false;
  String m=mode;
  boolean ok= (cmd.equals("ALARM")&&m.equals("NORMAL"))
    ||(cmd.equals("RESET")&&m.equals("ALARM"))
    ||(cmd.equals("OFF")&&m.equals("NORMAL"))
    ||(cmd.equals("ON")&&m.equals("OFF"))
    ||(cmd.startsWith("SET ")&&!m.equals("ALARM"))
    ||(cmd.startsWith("PREVIEW ")&&!m.equals("ALARM"))
    ||(cmd.equals("CANCEL")&&!m.equals("ALARM"));
  if(!ok)return false;
  // Only the latest unsent preview matters; avoid flooding GATT with slider updates.
  if(cmd.startsWith("PREVIEW ")){
   sendQueue.removeIf(q->q.startsWith("PREVIEW "));
  }
  if(cmd.equals("CANCEL")||cmd.startsWith("SET ")){
   sendQueue.removeIf(q->q.startsWith("PREVIEW "));
  }
  sendQueue.offer(cmd);drain();return true;
 }
 private void drain(){
  if(writing||reading||subscribing||!ready||gatt==null||commandCharacteristic==null||!permitted())return;
  String cmd=sendQueue.poll();if(cmd==null)return;
  writingCommand=cmd;
  writing=true;busy=true;message="Sending "+cmd.split(" ")[0]+"…";signal();
  byte[] payload=cmd.getBytes(StandardCharsets.UTF_8);
  int result=gatt.writeCharacteristic(commandCharacteristic,payload,BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
  if(result!=BluetoothStatusCodes.SUCCESS){
   writing=false;busy=false;writingCommand="";
   if(releasePending){releasePending=false;handler.removeCallbacks(releaseTimeout);handler.removeCallbacks(releaseProbe);}
   if(saveCallback!=null&&cmd.equals(saveCommand))finishSave(false,"BLE rejected the settings command.");
   message="Unable to send command ("+result+")";signal();
  }
 }
 @Override public void onDestroy(){
  active=false;
  handler.removeCallbacksAndMessages(null);stopScan();closeGatt();
  if(receiverRegistered){unregisterReceiver(bluetoothState);receiverRegistered=false;}
  super.onDestroy();
 }
}