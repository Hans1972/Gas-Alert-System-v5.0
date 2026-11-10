package nl.hbelectronics.hpgastest;

import android.Manifest;
import android.app.*;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Gas Alert v5.0 UI: preserves the big control and full-screen colors of v1.4. */
public final class MainActivity extends Activity {
 private final Handler h=new Handler(Looper.getMainLooper());
 private BluetoothAdapter adapter;
 private BluetoothLeScanner scanner;
 private HpBleService ble;
 private boolean scanning=false,bound=false;
 private final ArrayList<BluetoothDevice> devices=new ArrayList<>();
 private final ArrayList<String> labels=new ArrayList<>();
 private ArrayAdapter<String> listAdapter;
 private AlertDialog picker;
 private LinearLayout root,page;
 private Button connect,gas,power,settings;
 private int lastPageColor=Integer.MIN_VALUE;
 private TextView connection,signal,status,detail;
 private final HpBleService.Listener listener=()->render();
 private final ServiceConnection serviceConnection=new ServiceConnection(){
  @Override public void onServiceConnected(ComponentName name,IBinder binder){
   ble=((HpBleService.LocalBinder)binder).getService();bound=true;ble.setListener(listener);render();
  }
  @Override public void onServiceDisconnected(ComponentName name){ble=null;bound=false;render();}
 };
 private final Runnable scanEnd=()->stopScan();
 // Aligned animation tick: 500 ms blink phases must not be sampled every 600 ms.
 private static final long BLINK_HALF_PERIOD_MS=500L;
 private final Runnable refresher=new Runnable(){@Override public void run(){
  render();
  long now=SystemClock.elapsedRealtime();
  long untilNext=BLINK_HALF_PERIOD_MS-(now%BLINK_HALF_PERIOD_MS);
  h.postDelayed(this,untilNext);
 }};
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private LinearLayout.LayoutParams lp(int height){
  LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,height<0?height:dp(height));
  p.topMargin=dp(13);return p;
 }
 private TextView text(String s,int size){
  TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.WHITE);
  root.addView(t,lp(-2));return t;
 }
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
  root.setPadding(dp(24),dp(25),dp(24),dp(20));scroll.addView(root);
  page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
  TextView footer=new TextView(this);
  footer.setText("Made by HB-Electronics.nl · © 2026");footer.setTextSize(12);
  footer.setTextColor(Color.WHITE);footer.setGravity(Gravity.CENTER);
  footer.setPadding(dp(10),dp(8),dp(10),dp(12));
  page.addView(footer,new LinearLayout.LayoutParams(-1,-2));setContentView(page);
  page.setOnApplyWindowInsetsListener((v,insets)->{
   android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
   v.setPadding(0,bars.top,0,bars.bottom);return insets;
  });
  text("HP Gas Alert System",32).setTypeface(null,Typeface.BOLD);
  text("Version 5.0",15);
  connection=text("Bluetooth not connected",17);
  signal=text("Signal: unavailable (disconnected)",15);
  // Create button drawables once. Replacing them every animation frame causes
  // unnecessary view invalidation and visually unstable button surfaces.
  connect=makeButton("Connect Bluetooth",58,16);
  connect.setOnClickListener(v->connectClicked());
  gas=makeButton("HP GAS ALERT",225,27);gas.setTypeface(null,Typeface.BOLD);
  gas.setAutoSizeTextTypeUniformWithConfiguration(12,29,1,android.util.TypedValue.COMPLEX_UNIT_SP);
  gas.setOnClickListener(v->{
   if(ble==null)return;
   if("NORMAL".equals(ble.mode))ble.command("ALARM");
   else if("ALARM".equals(ble.mode))ble.command("RESET");
   else if("OFF".equals(ble.mode))ble.command("ON");
   render();
  });
  status=text("STATUS UNKNOWN",24);status.setGravity(Gravity.CENTER);status.setTypeface(null,Typeface.BOLD);
  detail=text("Connect to a Gas Alert v5.0 controller",15);detail.setGravity(Gravity.CENTER);
  power=makeButton("ALL LIGHTS OFF",55,16);
  power.setOnClickListener(v->{
   if(ble==null||!"NORMAL".equals(ble.mode))return;
   new AlertDialog.Builder(this).setTitle("Turn all lights off?")
    .setMessage("This is only possible when the controller confirms NORMAL (green).")
    .setNegativeButton("Cancel",null).setPositiveButton("Turn off",(d,w)->{if(ble!=null)ble.command("OFF");}).show();
  });
  settings=makeButton("⚙ Settings / Help",55,16);
  settings.setOnClickListener(v->showSettings());
  gas.setBackground(rounded(Color.argb(78,255,255,255)));
  power.setBackground(rounded(Color.argb(45,255,255,255)));
  settings.setBackground(rounded(Color.argb(35,255,255,255)));
  connect.setBackground(rounded(Color.argb(35,255,255,255)));
  BluetoothManager bm=getSystemService(BluetoothManager.class);
  adapter=bm==null?null:bm.getAdapter();
  render();
  // A clean install has a new phone token; controller ownership from a removed
  // installation cannot safely be overridden by Bluetooth alone.
  if(!getPreferences(MODE_PRIVATE).getBoolean("fresh_install_hint_shown",false)){
   getPreferences(MODE_PRIVATE).edit().putBoolean("fresh_install_hint_shown",true).apply();
   h.post(()->new AlertDialog.Builder(this)
    .setTitle("Fresh installation · v5.0")
    .setMessage("This v5.0 app installs separately from v3.0. Do not uninstall v3.0. Tap Connect Bluetooth. If the controller remembers the old installation, it automatically makes the owner slot available after a maximum of 30 minutes without the old owner. The v5.0 app will retry automatically; no BOOT button or serial monitor is needed. Keep the controller powered on. LED and buzzer settings remain stored.")
    .setPositiveButton("Understood",null).show());
  }
 }
 private Button makeButton(String label,int height,int size){
  Button b=new Button(this);b.setText(label);b.setTextSize(size);b.setAllCaps(false);
  b.setTextColor(Color.WHITE);root.addView(b,lp(height));return b;
 }
 private boolean permitted(){return checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED
   &&checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)==PackageManager.PERMISSION_GRANTED;}
 private void connectClicked(){
  if(ble!=null&&ble.hasSelection()){
   if(!ble.ready){
    new AlertDialog.Builder(this).setTitle("Controller not connected")
     .setMessage("The controller must be connected to release ownership. Automatic reconnect remains active.")
     .setPositiveButton("OK",null).show();return;
   }
   new AlertDialog.Builder(this).setTitle("Release controller")
    .setMessage("Release the controller for another phone and disconnect? Current LED and alarm state will not change.")
    .setNegativeButton("Cancel",null).setPositiveButton("Release & disconnect",(d,w)->{
     startService(new Intent(this,HpBleService.class).setAction(HpBleService.ACTION_DISCONNECT));
     h.postDelayed(this::render,200);
    }).show();return;
  }
  if(adapter==null){connection.setText("Bluetooth is not supported");return;}
  if(!permitted()){requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT},10);return;}
  if(!adapter.isEnabled()){startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),11);return;}
  scan();
 }
 @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==10&&permitted())connectClicked();}
 @Override protected void onActivityResult(int r,int result,Intent data){super.onActivityResult(r,result,data);if(r==11&&adapter!=null&&adapter.isEnabled())scan();}
 private void scan(){
  if(scanning||adapter==null||!permitted())return;
  scanner=adapter.getBluetoothLeScanner();if(scanner==null)return;
  devices.clear();labels.clear();
  listAdapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,labels);
  picker=new AlertDialog.Builder(this).setTitle("Select HP Gas Alert v5.0 controller")
   .setAdapter(listAdapter,(d,index)->{
    BluetoothDevice device=devices.get(index);stopScan();
    Intent i=new Intent(this,HpBleService.class).setAction(HpBleService.ACTION_CONNECT)
     .putExtra(HpBleService.EXTRA_ADDRESS,device.getAddress())
     .putExtra(HpBleService.EXTRA_NAME,"HP Gas Alert System v5.0");
    startForegroundService(i);
   }).setNegativeButton("Cancel",(d,w)->stopScan()).create();
  picker.setOnDismissListener(d->stopScan());picker.show();
  try{
   scanning=true;scanner.startScan(
    Collections.singletonList(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(HpBleService.SERVICE)).build()),
    new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),scanCallback);
   h.postDelayed(scanEnd,15000);
  }catch(Exception e){stopScan();connection.setText("Bluetooth scan failed");}
 }
 private void stopScan(){
  h.removeCallbacks(scanEnd);
  if(scanning&&scanner!=null&&permitted())try{scanner.stopScan(scanCallback);}catch(Exception ignored){}
  scanning=false;
 }
 private final ScanCallback scanCallback=new ScanCallback(){
  @Override public void onScanResult(int type,ScanResult result){h.post(()->{
   if(!scanning||result==null||result.getDevice()==null)return;
   BluetoothDevice d=result.getDevice();
   for(BluetoothDevice x:devices)if(x.getAddress().equals(d.getAddress()))return;
   devices.add(d);labels.add("HP Gas Alert System v5.0  ·  "+result.getRssi()+" dBm");
   listAdapter.notifyDataSetChanged();
  });}
  @Override public void onScanFailed(int code){h.post(()->{stopScan();connection.setText("Scan error "+code);});}
 };
 private GradientDrawable rounded(int argb){
  GradientDrawable d=new GradientDrawable();d.setColor(argb);d.setCornerRadius(dp(22));
  d.setStroke(dp(2),Color.argb(140,255,255,255));return d;
 }
 private void render(){
  if(gas==null)return;
  boolean connected=ble!=null&&ble.ready&&ble.revision>=0;
  String m=connected?ble.mode:"UNKNOWN";
  int bg;
  if(!connected)bg=(SystemClock.elapsedRealtime()/BLINK_HALF_PERIOD_MS)%2==0?Color.rgb(179,24,44):Color.rgb(94,18,31);
  else if("ALARM".equals(m))bg=(SystemClock.elapsedRealtime()/BLINK_HALF_PERIOD_MS)%2==0?Color.rgb(182,86,7):Color.rgb(106,49,2);
  else if(ble.busy)bg=Color.rgb(69,79,98);
  else if("NORMAL".equals(m))bg=Color.rgb(19,102,62);
  else bg=Color.rgb(38,51,70);
  if(bg!=lastPageColor){
   lastPageColor=bg;
   page.setBackgroundColor(bg);
   getWindow().setStatusBarColor(bg);
   getWindow().setNavigationBarColor(bg);
  }
  if(!connected){
   gas.setText("HP GAS ALERT\nNOT CONNECTED");status.setText("STATUS UNKNOWN");
   connection.setText(ble!=null?ble.message:"Bluetooth not connected");
   detail.setText(ble!=null&&ble.hasSelection()?"Automatic Bluetooth reconnect is active":"Select the Gas Alert v5.0 controller.");
  }else{
   if("ALARM".equals(m)){gas.setText("HP GAS ALERT ON\nTAP TO RESET");status.setText("ALARM · ORANGE");}
   else if("NORMAL".equals(m)){gas.setText("HP GAS ALERT OFF\nTAP TO START");status.setText("NORMAL · GREEN");}
   else{gas.setText("LIGHTS OFF\nTAP TO TURN ON");status.setText("OFF");}
   connection.setText(ble.message);detail.setText("Controller confirmed · state revision "+ble.revision);
  }
  signal.setText(ble==null?"Signal: unavailable (disconnected)":ble.signalLabel());
  gas.setEnabled(connected&&!ble.busy);
  power.setEnabled(connected&&!ble.busy&&"NORMAL".equals(m));
  power.setAlpha(power.isEnabled()?1f:.45f);
  connect.setText(ble!=null&&ble.hasSelection()?"Disconnect / Change device":scanning?"Scanning…":"Connect Bluetooth");
  connect.setEnabled(!scanning);
  settings.setEnabled(connected&&!ble.busy);
  settings.setAlpha(settings.isEnabled()?1f:.5f);
 }
 private void showSettings(){
  if(ble==null||!ble.ready)return;
  ScrollView scroll=new ScrollView(this);
  LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
  content.setPadding(dp(20),dp(12),dp(20),dp(18));scroll.addView(content);
  String[] keys={"greenBrightness","leds","layout","effect","brightness","tint","speed","afterglow","colorMode","buzzerMode","buzzerSeconds","pulseOnMs","pulseOffMs"};
  String[] names={"Green brightness (%)","Number of physical LEDs","LED layout (0 single, 1 double, 2 matrix)","Orange effect (1–40)","Orange brightness (%)","Orange hue (0–100)","Animation speed (25–200%)","Afterglow (0–100)","Color order (0–5)","Buzzer (0 continuous, 1 pulsed)","Buzzer duration (seconds)","Pulse ON (milliseconds)","Pulse OFF (milliseconds)"};
  for(int i=0;i<keys.length;i++){
   final String key=keys[i],title=names[i];
   Button b=new Button(this);b.setAllCaps(false);b.setText(title+"\n"+displayValue(key,valueOf(ble.config,key)));
   content.addView(b,new LinearLayout.LayoutParams(-1,dp(62)));
   b.setOnClickListener(v->editSetting(key,title,b));
  }
  Button help=new Button(this);help.setAllCaps(false);help.setText("▸ Help and safety information");
  content.addView(help,new LinearLayout.LayoutParams(-1,dp(52)));
  TextView info=new TextView(this);info.setTextColor(Color.DKGRAY);info.setTextSize(14);
  info.setText("NORMAL: steady green. ALARM: saved orange effect and buzzer. ALL OFF is allowed only when NORMAL is confirmed.\n\nBluetooth: the owning phone reconnects automatically. The prototype firmware reserves ownership for 30 minutes after connection loss, but is NOT yet cryptographically authenticated.\n\nFresh installation: if the controller still remembers an old owner, it will automatically release the reservation after 30 minutes without that owner. This app will reconnect automatically. Do not reset the device or remove app data while waiting.\n\nThis app is a bench-test prototype, not certified gas safety equipment. Never rely on BLE as the only warning path.");
  info.setPadding(dp(10),dp(8),dp(10),dp(12));info.setVisibility(View.GONE);
  content.addView(info);
  Button licensing=new Button(this);
  licensing.setAllCaps(false);
  licensing.setText("🔑 Licentiebeheer · HB-Electronics.nl");
  LinearLayout.LayoutParams licenseLp=new LinearLayout.LayoutParams(-1,dp(60));
  licenseLp.topMargin=dp(22);
  content.addView(licensing,licenseLp);
  licensing.setOnClickListener(v->showLicenseManager());
  help.setOnClickListener(v->{boolean show=info.getVisibility()!=View.VISIBLE;info.setVisibility(show?View.VISIBLE:View.GONE);help.setText(show?"▾ Help and safety information":"▸ Help and safety information");});
  new AlertDialog.Builder(this).setTitle("HP Gas Alert System · Settings v5.0")
   .setView(scroll).setPositiveButton("Close",null).show();
 }
 private void showLicenseManager(){
  HbLicenseManager manager=new HbLicenseManager(this);
  LinearLayout layout=new LinearLayout(this);
  layout.setOrientation(LinearLayout.VERTICAL);
  layout.setPadding(dp(22),dp(16),dp(22),dp(10));
  TextView header=new TextView(this);
  header.setText("HB-Electronics.nl · productcode hb-gas-alert-v5");
  header.setTextColor(Color.LTGRAY);header.setTextSize(14);
  layout.addView(header);
  TextView statusText=new TextView(this);
  statusText.setText(manager.status());statusText.setTextColor(Color.WHITE);
  statusText.setTextSize(17);statusText.setTypeface(null,Typeface.BOLD);
  statusText.setPadding(0,dp(12),0,dp(16));
  layout.addView(statusText);
  EditText licenseInput=new EditText(this);
  licenseInput.setSingleLine(true);
  licenseInput.setHint("Licentiecode invoeren");
  licenseInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
  layout.addView(licenseInput);
  Button activate=new Button(this);activate.setAllCaps(false);activate.setText("Licentie activeren");
  layout.addView(activate);
  Button trial=new Button(this);trial.setAllCaps(false);trial.setText("Proefperiode starten");
  layout.addView(trial);
  Button check=new Button(this);check.setAllCaps(false);check.setText("Licentie controleren");
  layout.addView(check);
  TextView note=new TextView(this);note.setTextColor(Color.LTGRAY);note.setTextSize(13);
  note.setText("De licentie hoort bij deze Android-app, niet bij de ESP32-C3. Licentiecontrole verandert nooit de alarmstatus of BLE-verbinding.");
  note.setPadding(0,dp(12),0,0);layout.addView(note);
  final boolean[] running={false};
  final AlertDialog dialog=new AlertDialog.Builder(this)
   .setTitle("Licentiebeheer · v5.0")
   .setView(layout).setPositiveButton("Sluiten",null).create();
  class Actions {
   void run(String action){
    if(running[0])return;
    String code=licenseInput.getText().toString().trim();
    if("activate".equals(action)&&code.isEmpty()){
     Toast.makeText(MainActivity.this,"Vul eerst je licentiecode in.",Toast.LENGTH_LONG).show();
     return;
    }
    running[0]=true;activate.setEnabled(false);trial.setEnabled(false);check.setEnabled(false);
    statusText.setText("Licentieserver controleren…");
    if(!manager.request(action,code,(ok,message)->{
     if(!dialog.isShowing())return;
     running[0]=false;activate.setEnabled(true);trial.setEnabled(true);check.setEnabled(true);
     statusText.setText(manager.status());
     showLicenseNotice(ok,message);
    })){
     running[0]=false;activate.setEnabled(true);trial.setEnabled(true);check.setEnabled(true);
     statusText.setText(manager.status());
     Toast.makeText(MainActivity.this,"Licentiecontrole is al bezig.",Toast.LENGTH_LONG).show();
    }
   }
  }
  Actions actions=new Actions();
  activate.setOnClickListener(v->actions.run("activate"));
  trial.setOnClickListener(v->actions.run("trial"));
  check.setOnClickListener(v->actions.run("check"));
  dialog.show();
  // Refresh status from the server when license management is opened.
  // Never claim that a cached proof reflects a just-checked online status.
  if(!"Nog niet geactiveerd".equals(manager.status())){
   running[0]=true;
   activate.setEnabled(false);trial.setEnabled(false);check.setEnabled(false);
   String lastKnown=manager.status();
   statusText.setText("Licentiestatus controleren…\nLaatste bekende status: "+lastKnown);
   if(!manager.request("check","",(ok,message)->{
    if(!dialog.isShowing())return;
    running[0]=false;activate.setEnabled(true);trial.setEnabled(true);check.setEnabled(true);
    statusText.setText(manager.status());
    if(!ok && manager.status().equals(lastKnown)){
     // A failed network request is not proof of revocation; make uncertainty clear.
     statusText.setText("Laatste bekende status: "+lastKnown+"\nOnline controle niet bevestigd.");
    }
   })){
    running[0]=false;activate.setEnabled(true);trial.setEnabled(true);check.setEnabled(true);
    statusText.setText(lastKnown);
   }
  }
 }
 private String valueOf(String cfg,String key){
  if(cfg==null)return "?";
  for(String item:cfg.split(";")){String[] pair=item.split("=",2);if(pair.length==2&&key.equals(pair[0]))return pair[1];}
  return "?";
 }
 private String displayValue(String key,String raw){
  if(raw==null||"?".equals(raw)||raw.isEmpty())return "Value unavailable";
  switch(key){
   case "greenBrightness":case "brightness":case "tint":case "afterglow":case "speed":
    return raw+"%";
   case "buzzerSeconds":return raw+" s";
   case "pulseOnMs":case "pulseOffMs":return raw+" ms";
   case "effect":return "Effect "+raw+" / 40";
   case "leds":return raw+" LEDs";
   case "layout":return "0".equals(raw)?"Single":"1".equals(raw)?"Double mirrored":"8 × 32 matrix";
   case "buzzerMode":return "0".equals(raw)?"Continuous":"Pulsed";
   default:return raw;
  }
 }
 private int[] sliderRange(String key){
  switch(key){
   case "greenBrightness":return new int[]{0,100,1};
   case "brightness":return new int[]{5,100,1};
   case "tint":return new int[]{0,100,1};
   case "speed":return new int[]{25,200,1};
   case "afterglow":return new int[]{0,100,1};
   case "buzzerSeconds":return new int[]{0,120,1};
   case "pulseOnMs":case "pulseOffMs":return new int[]{50,5000,50};
   case "effect":return new int[]{1,40,1};
   default:return null;
  }
 }
 private void showSettingNotice(boolean success,String title,String body){
  if(isFinishing()||isDestroyed())return;
  int color=success?Color.rgb(42,169,103):Color.rgb(210,91,47);
  LinearLayout content=new LinearLayout(this);
  content.setOrientation(LinearLayout.VERTICAL);
  content.setPadding(dp(24),dp(12),dp(24),dp(10));
  TextView heading=new TextView(this);
  heading.setText(success?"✓ Saved successfully":"Setting not saved");
  heading.setTextSize(19);heading.setTypeface(null,Typeface.BOLD);
  heading.setTextColor(color);
  content.addView(heading);
  TextView bodyText=new TextView(this);
  bodyText.setText(body);
  bodyText.setTextSize(15);bodyText.setTextColor(Color.WHITE);
  bodyText.setPadding(0,dp(12),0,dp(6));
  content.addView(bodyText);
  AlertDialog popup=new AlertDialog.Builder(this)
   .setTitle(title).setView(content).setPositiveButton("OK",null).create();
  popup.setOnShowListener(d->popup.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color));
  popup.show();
 }
 private void showLicenseNotice(boolean success,String message){
  if(isFinishing()||isDestroyed())return;
  boolean revoked=!success&&message.startsWith("Deze licentie is ingetrokken");
  int color=success?Color.rgb(42,169,103):Color.rgb(210,91,47);
  LinearLayout content=new LinearLayout(this);
  content.setOrientation(LinearLayout.VERTICAL);
  content.setPadding(dp(24),dp(12),dp(24),dp(10));
  TextView heading=new TextView(this);
  heading.setText(success?"✓ Licentie gecontroleerd":revoked?"Licentie ingetrokken":"Licentiecontrole mislukt");
  heading.setTextSize(19);
  heading.setTypeface(null,Typeface.BOLD);
  heading.setTextColor(color);
  content.addView(heading);
  TextView explanation=new TextView(this);
  explanation.setText(message);
  explanation.setTextColor(Color.WHITE);
  explanation.setTextSize(15);
  explanation.setPadding(0,dp(12),0,dp(6));
  content.addView(explanation);
  AlertDialog popup=new AlertDialog.Builder(this)
   .setTitle("HB-Electronics · Licentiebeheer")
   .setView(content).setPositiveButton("OK",null).create();
  popup.setOnShowListener(d->popup.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color));
  popup.show();
 }
 private boolean saveSettingWithFeedback(String key,String raw,String title,Button origin){
  if(ble==null||!ble.ready){
   showSettingNotice(false,title,"Controller is not connected.");
   return false;
  }
  if(!"NORMAL".equals(ble.mode)){
   showSettingNotice(false,title,"Settings can only be changed in NORMAL (green). Reset the orange alarm first.");
   return false;
  }
  boolean accepted=ble.saveSetting(key,raw,(confirmed,detail)->runOnUiThread(()->{
   if(confirmed){
    origin.setText(title+"\n"+displayValue(key,raw));
    showSettingNotice(true,title,"The controller confirmed "+displayValue(key,raw)+" in NORMAL (green).");
   }else{
    showSettingNotice(false,title,detail);
   }
  }));
  if(!accepted)showSettingNotice(false,title,"Controller busy or not ready to save. Please retry.");
  return accepted;
 }
 private void editSetting(String key,String title,Button origin){
  if(ble==null||!ble.ready)return;
  if(!"NORMAL".equals(ble.mode)){
   showSettingNotice(false,title,"Settings are blocked while the orange alarm is active. Reset the alarm to NORMAL (green) before changing settings.");
   return;
  }
  int[] range=sliderRange(key);
  if(range!=null){
   int lower=range[0],upper=range[1],step=range[2],initial=lower;
   try{initial=Integer.parseInt(valueOf(ble.config,key));}catch(Exception ignored){}
   initial=Math.max(lower,Math.min(upper,initial));
   LinearLayout layout=new LinearLayout(this);
   layout.setOrientation(LinearLayout.VERTICAL);
   layout.setPadding(dp(22),dp(8),dp(22),dp(0));
   // Compact layout with value aligned to the right, no oversized dark box.
   LinearLayout valueRow=new LinearLayout(this);
   valueRow.setOrientation(LinearLayout.HORIZONTAL);
   valueRow.setGravity(Gravity.CENTER_VERTICAL);
   TextView hint=new TextView(this);
   hint.setText("Selected value");
   hint.setTextSize(13);
   hint.setTextColor(Color.LTGRAY);
   hint.setGravity(Gravity.CENTER_VERTICAL);
   valueRow.addView(hint,new LinearLayout.LayoutParams(0,dp(42),1f));
   TextView value=new TextView(this);
   value.setTextSize(23);
   value.setTypeface(null,Typeface.BOLD);
   value.setTextColor(Color.WHITE);
   value.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
   value.setContentDescription("Selected slider value");
   valueRow.addView(value,new LinearLayout.LayoutParams(-2,dp(42)));
   layout.addView(valueRow,new LinearLayout.LayoutParams(-1,dp(44)));
   SeekBar slider=new SeekBar(this);
   if(android.os.Build.VERSION.SDK_INT>=29){
    slider.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(49,183,134)));
    slider.setThumbTintList(android.content.res.ColorStateList.valueOf(Color.rgb(49,183,134)));
   }
   layout.addView(slider,new LinearLayout.LayoutParams(-1,dp(50)));
   int steps=(upper-lower)/step;
   slider.setMax(steps);slider.setProgress((initial-lower)/step);
   value.setText(displayValue(key,String.valueOf(lower+slider.getProgress()*step)));
   final boolean[] saved={false};
   final Runnable[] pending={null};
   final int minValue=lower,stepValue=step;
   final String previewKey=key;
   final Runnable sendPreview=()->{
    if(ble!=null&&ble.ready){
     int selected=minValue+slider.getProgress()*stepValue;
     ble.command("PREVIEW "+previewKey+"="+selected);
    }
   };
   slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
    @Override public void onProgressChanged(SeekBar bar,int progress,boolean user){
     int selected=minValue+progress*stepValue;
     value.setText(displayValue(key,String.valueOf(selected)));
     if(user){
      if(pending[0]!=null)h.removeCallbacks(pending[0]);
      pending[0]=sendPreview;
      h.postDelayed(pending[0],180); // throttle BLE, not the UI value
     }
    }
    @Override public void onStartTrackingTouch(SeekBar bar){}
    @Override public void onStopTrackingTouch(SeekBar bar){
     if(pending[0]!=null)h.removeCallbacks(pending[0]);
     pending[0]=sendPreview;
     h.postDelayed(pending[0],100);
    }
   });
   // Value and slider views already added in the compact row above.
   AlertDialog dialog=new AlertDialog.Builder(this).setTitle(title).setView(layout)
    .setNegativeButton("Cancel",(d,w)->{
     if(pending[0]!=null)h.removeCallbacks(pending[0]);
     if(ble!=null)ble.command("CANCEL");
    })
    .setPositiveButton("Save",(d,w)->{
     if(pending[0]!=null)h.removeCallbacks(pending[0]);
     int selected=minValue+slider.getProgress()*stepValue;
     saved[0]=saveSettingWithFeedback(key,String.valueOf(selected),title,origin);
    }).create();
   dialog.setOnDismissListener(d->{
    if(pending[0]!=null)h.removeCallbacks(pending[0]);
    // Back button or outside tap must also roll back an uncommitted preview.
    if(!saved[0]&&ble!=null)ble.command("CANCEL");
   });
   dialog.show();
   return;
  }
  if("layout".equals(key)||"colorMode".equals(key)||"buzzerMode".equals(key)){
   final String[] labels;
   if("layout".equals(key))labels=new String[]{"Single strip","Double mirrored strip","8 × 32 matrix"};
   else if("buzzerMode".equals(key))labels=new String[]{"Continuous","Pulsed"};
   else labels=new String[]{"GRB","RGB","RBG","GBR","BRG","BGR"};
   int current=0;try{current=Integer.parseInt(valueOf(ble.config,key));}catch(Exception ignored){}
   current=Math.max(0,Math.min(labels.length-1,current));
   new AlertDialog.Builder(this).setTitle(title).setSingleChoiceItems(labels,current,(d,which)->{
    d.dismiss();
    saveSettingWithFeedback(key,String.valueOf(which),title,origin);
   }).setNegativeButton("Cancel",null).show();
   return;
  }
  EditText edit=new EditText(this);edit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
  String initial=valueOf(ble.config,key);if(!"?".equals(initial))edit.setText(initial);
  new AlertDialog.Builder(this).setTitle(title).setView(edit)
   .setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{
    String val=edit.getText().toString().trim();
    if(!val.matches("[0-9]{1,5}")){Toast.makeText(this,"Enter a valid number",Toast.LENGTH_LONG).show();return;}
    saveSettingWithFeedback(key,val,title,origin);
   }).show();
 }
 @Override protected void onStart(){super.onStart();bindService(new Intent(this,HpBleService.class),serviceConnection,BIND_AUTO_CREATE);h.post(refresher);}
 @Override protected void onStop(){
  super.onStop();h.removeCallbacks(refresher);stopScan();if(picker!=null)picker.dismiss();
  if(bound){ble.setListener(null);unbindService(serviceConnection);bound=false;ble=null;}
 }
}