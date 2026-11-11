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
 private String tr(String english){return UiLanguage.t(this,english);}
 private String currentLicenseStatus(){return UiLanguage.licenseStatus(this,licenseManager.status());}
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
 private TextView connection,signal,status,detail,licenseStatus;
 private HbLicenseManager licenseManager;
 private boolean licenseChecking=false;
 private String licenseWarningShownFor="";
 private int lastLicenseBannerColor=Integer.MIN_VALUE;
 private String lastLicenseBannerText="";
 private AlertDialog licenseBlockedDialog;
 private long lastLicenseCheckAt=0L;
 private static final long LICENSE_CHECK_INTERVAL_MS=5L*60L*1000L;
 private final HpBleService.Listener listener=()->render();
 private final ServiceConnection serviceConnection=new ServiceConnection(){
  @Override public void onServiceConnected(ComponentName name,IBinder binder){
   ble=((HpBleService.LocalBinder)binder).getService();bound=true;ble.setListener(listener);render();
  }
  @Override public void onServiceDisconnected(ComponentName name){ble=null;bound=false;render();}
 };
 private final Runnable scanEnd=()->{
  stopScan();
  if(devices.isEmpty()&&picker!=null&&picker.isShowing()&&listAdapter!=null){
   labels.clear();
   labels.add(tr("No controller found. Check the ESP32-C3 and close the v3 app."));
   labels.add(tr("Tap 'Search again' to scan again."));
   listAdapter.notifyDataSetChanged();
  }
 };
 // Aligned animation tick: 500 ms blink phases must not be sampled every 600 ms.
 private static final long BLINK_HALF_PERIOD_MS=500L;
 private final Runnable refresher=new Runnable(){@Override public void run(){
  render();
  if(licenseManager!=null)checkLicenseOnStart();
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
  footer.setText(tr("Made by HB-Electronics.nl · © 2026"));footer.setTextSize(12);
  footer.setTextColor(Color.WHITE);footer.setGravity(Gravity.CENTER);
  footer.setPadding(dp(10),dp(8),dp(10),dp(12));
  page.addView(footer,new LinearLayout.LayoutParams(-1,-2));setContentView(page);
  page.setOnApplyWindowInsetsListener((v,insets)->{
   android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
   v.setPadding(0,bars.top,0,bars.bottom);return insets;
  });
  text("HP Gas Alert System",32).setTypeface(null,Typeface.BOLD);
  text(tr("Version")+" 5.0",15);
  licenseManager=new HbLicenseManager(this);
  licenseStatus=text(tr("Checking license status…"),14);
  licenseStatus.setPadding(dp(12),dp(8),dp(12),dp(8));
  licenseStatus.setTypeface(null,Typeface.BOLD);
  licenseStatus.setOnClickListener(v->showLicenseManager());
  connection=text(tr("Bluetooth not connected"),17);
  signal=text(tr("Signal: unavailable (disconnected)"),15);
  // Create button drawables once. Replacing them every animation frame causes
  // unnecessary view invalidation and visually unstable button surfaces.
  connect=makeButton(tr("Connect Bluetooth"),58,16);
  connect.setOnClickListener(v->connectClicked());
  gas=makeButton("HP GAS ALERT",225,27);gas.setTypeface(null,Typeface.BOLD);
  gas.setAutoSizeTextTypeUniformWithConfiguration(12,29,1,android.util.TypedValue.COMPLEX_UNIT_SP);
  gas.setOnClickListener(v->{
   if(!allowLicensedControl())return;
   if(ble==null)return;
   if("NORMAL".equals(ble.mode))ble.command("ALARM");
   else if("ALARM".equals(ble.mode))ble.command("RESET");
   else if("OFF".equals(ble.mode))ble.command("ON");
   render();
  });
  status=text(tr("STATUS UNKNOWN"),24);status.setGravity(Gravity.CENTER);status.setTypeface(null,Typeface.BOLD);
  detail=text(tr("Connect to a Gas Alert v5.0 controller"),15);detail.setGravity(Gravity.CENTER);
  power=makeButton(tr("ALL LIGHTS OFF"),55,16);
  power.setOnClickListener(v->{
   if(!allowLicensedControl())return;
   if(ble==null||!"NORMAL".equals(ble.mode))return;
   new AlertDialog.Builder(this).setTitle(tr("Turn all lights off?"))
    .setMessage(tr("This is only possible when the controller confirms NORMAL (green)."))
    .setNegativeButton(tr("Cancel"),null).setPositiveButton(tr("Turn off"),(d,w)->{if(ble!=null)ble.command("OFF");}).show();
  });
  settings=makeButton("⚙ "+tr("Settings"),55,16);
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
 }
 private Button makeButton(String label,int height,int size){
  Button b=new Button(this);b.setText(label);b.setTextSize(size);b.setAllCaps(false);
  b.setTextColor(Color.WHITE);root.addView(b,lp(height));return b;
 }
 private boolean permitted(){return checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED
   &&checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)==PackageManager.PERMISSION_GRANTED;}
 private void connectClicked(){
  // An existing owner can always disconnect/release the controller.
  if(ble==null||!ble.hasSelection()){
   if(!allowLicensedControl())return;
  }
  if(ble!=null&&ble.hasSelection()){
   if(!ble.ready){
    new AlertDialog.Builder(this)
     .setTitle(tr("Controller not connected"))
     .setMessage(tr("The app remembers a previously selected controller, but Bluetooth is currently disconnected.\n\nTry reconnecting, or choose another controller. This changes only the saved selection on this phone, not the ownership reservation on the ESP32."))
     .setPositiveButton(tr("Reconnect"),(d,w)->{
      startService(new Intent(this,HpBleService.class).setAction(HpBleService.ACTION_RECONNECT));
     })
     .setNeutralButton(tr("Choose another controller"),(d,w)->{
      startService(new Intent(this,HpBleService.class).setAction(HpBleService.ACTION_CLEAR_SELECTION));
      h.postDelayed(()->{
       render();
       if(adapter!=null&&permitted()&&adapter.isEnabled())scan();
       else connectClicked();
      },400);
     })
     .setNegativeButton(tr("Cancel"),null).show();
    return;
   }
   new AlertDialog.Builder(this).setTitle(tr("Release controller"))
    .setMessage(tr("Release the controller for another phone and disconnect? Current LED and alarm state will not change."))
    .setNegativeButton(tr("Cancel"),null).setPositiveButton(tr("Release & disconnect"),(d,w)->{
     startService(new Intent(this,HpBleService.class).setAction(HpBleService.ACTION_DISCONNECT));
     h.postDelayed(this::render,200);
    }).show();return;
  }
  if(adapter==null){connection.setText(tr("Bluetooth is not supported"));return;}
  if(!permitted()){requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT},10);return;}
  if(!adapter.isEnabled()){startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),11);return;}
  scan();
 }
 @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==10&&permitted())connectClicked();}
 @Override protected void onActivityResult(int r,int result,Intent data){super.onActivityResult(r,result,data);if(r==11&&adapter!=null&&adapter.isEnabled())scan();}
 private String bleName(ScanResult result){
  if(result==null)return "";
  ScanRecord record=result.getScanRecord();
  String fromRecord=record==null?null:record.getDeviceName();
  if(fromRecord!=null&&!fromRecord.trim().isEmpty())return fromRecord.trim();
  if(permitted()){
   try{
    String fromDevice=result.getDevice().getName();
    if(fromDevice!=null)return fromDevice.trim();
   }catch(SecurityException ignored){}
  }
  return "";
 }
 private boolean isGasAlertController(ScanResult result){
  if(result==null||result.getDevice()==null)return false;
  ScanRecord record=result.getScanRecord();
  if(record!=null){
   List<ParcelUuid> uuids=record.getServiceUuids();
   if(uuids!=null&&uuids.contains(new ParcelUuid(HpBleService.SERVICE)))return true;
  }
  String name=bleName(result).toLowerCase(Locale.ROOT);
  return name.contains("hp gas alert")||name.contains("gas alert system");
 }
 private void scan(){
  if(scanning||adapter==null||!permitted())return;
  scanner=adapter.getBluetoothLeScanner();
  if(scanner==null){
   new AlertDialog.Builder(this).setTitle(tr("Bluetooth unavailable"))
    .setMessage(tr("Turn on Bluetooth and try again.")).setPositiveButton("OK",null).show();
   return;
  }
  devices.clear();labels.clear();
  labels.add(tr("Search for HP Gas Alert controller…"));
  listAdapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,labels);
  picker=new AlertDialog.Builder(this).setTitle(tr("Search for HP Gas Alert controller"))
   .setAdapter(listAdapter,(d,index)->{
    if(index<0||index>=devices.size())return;
    BluetoothDevice device=devices.get(index);stopScan();
    Intent i=new Intent(this,HpBleService.class).setAction(HpBleService.ACTION_CONNECT)
     .putExtra(HpBleService.EXTRA_ADDRESS,device.getAddress())
     .putExtra(HpBleService.EXTRA_NAME,"HP Gas Alert System v5.0");
    startForegroundService(i);
   }).setNeutralButton(tr("Search again"),(d,w)->{
    stopScan();h.postDelayed(this::scan,300);
   }).setNegativeButton(tr("Close"),(d,w)->stopScan()).create();
  picker.setOnDismissListener(d->stopScan());picker.show();
  try{
   scanning=true;
   // Unfiltered scan: the 128-bit service UUID can appear in the scan response
   // instead of the primary advertisement on some ESP32/Android combinations.
   // UI performs a strict service-or-device-name check before listing devices.
   scanner.startScan(null,
    new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),scanCallback);
   h.postDelayed(scanEnd,15000);
  }catch(Exception e){
   stopScan();
   if(picker!=null&&picker.isShowing()){
    labels.clear();labels.add(tr("Bluetooth search failed. Tap 'Search again'."));
    listAdapter.notifyDataSetChanged();
   }
  }
 }
 private void stopScan(){
  h.removeCallbacks(scanEnd);
  if(scanning&&scanner!=null&&permitted())try{scanner.stopScan(scanCallback);}catch(Exception ignored){}
  scanning=false;
 }
 private final ScanCallback scanCallback=new ScanCallback(){
  @Override public void onScanResult(int type,ScanResult result){h.post(()->{
   if(!scanning||!isGasAlertController(result))return;
   BluetoothDevice d=result.getDevice();
   for(BluetoothDevice x:devices)if(x.getAddress().equals(d.getAddress()))return;
   if(devices.isEmpty())labels.clear();
   devices.add(d);
   String displayName=bleName(result);
   if(displayName.isEmpty())displayName="HP Gas Alert controller";
   labels.add(displayName+" · "+result.getRssi()+" dBm");
   listAdapter.notifyDataSetChanged();

  });}
  @Override public void onScanFailed(int code){h.post(()->{
   stopScan();
   if(picker!=null&&picker.isShowing()){
    labels.clear();labels.add(tr("Bluetooth search failed. Tap 'Search again'.")+" ("+code+")");
    listAdapter.notifyDataSetChanged();
   }
  });}
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
   gas.setText(tr("HP GAS ALERT\nNOT CONNECTED"));status.setText(tr("STATUS UNKNOWN"));
   connection.setText(ble!=null?UiLanguage.ble(this,ble.message):tr("Bluetooth not connected"));
   detail.setText(ble!=null&&ble.hasSelection()?tr("Automatic Bluetooth reconnect is active"):tr("Select the Gas Alert v5.0 controller."));
   detail.setVisibility(View.VISIBLE);
  }else{
   if("ALARM".equals(m)){gas.setText(tr("HP GAS ALERT ON\nTAP TO RESET"));status.setText(tr("ALARM · ORANGE"));}
   else if("NORMAL".equals(m)){gas.setText(tr("HP GAS ALERT OFF\nTAP TO START"));status.setText(tr("NORMAL · GREEN"));}
   else{gas.setText(tr("LIGHTS OFF\nTAP TO TURN ON"));status.setText(tr("OFF"));}
   connection.setText(UiLanguage.ble(this,ble.message));detail.setVisibility(View.GONE);
  }
  signal.setText(ble==null?tr("Signal: unavailable (disconnected)"):UiLanguage.ble(this,ble.signalLabel()));
  refreshLicenseLabel();
  boolean licenseOk=licenseManager!=null&&licenseManager.hasValidLicense();
  gas.setEnabled(licenseOk&&connected&&!ble.busy);
  power.setEnabled(licenseOk&&connected&&!ble.busy&&"NORMAL".equals(m));
  power.setAlpha(power.isEnabled()?1f:.45f);
  connect.setText(ble!=null&&ble.hasSelection()
   ?(connected?tr("Disconnect / Change device"):tr("Reconnect / Change device"))
   :scanning?tr("Scanning…"):tr("Connect Bluetooth"));
  connect.setEnabled(!scanning&&(licenseOk||(ble!=null&&ble.hasSelection())));
  settings.setEnabled(true);settings.setAlpha(1f);
 }
 private void showSettings(){
  ScrollView scroll=new ScrollView(this);
  LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
  content.setPadding(dp(20),dp(12),dp(20),dp(18));scroll.addView(content);
  String[] keys={"greenBrightness","leds","layout","effect","brightness","tint","speed","afterglow","colorMode","buzzerMode","buzzerSeconds","pulseOnMs","pulseOffMs"};
  String[] names={"Green brightness (%)","Number of physical LEDs","LED layout (0 single, 1 double, 2 matrix)","Orange effect (1–40)","Orange brightness (%)","Orange hue (0–100)","Animation speed (25–200%)","Afterglow (0–100)","Color order (0–5)","Buzzer (0 continuous, 1 pulsed)","Buzzer duration (seconds)","Pulse ON (milliseconds)","Pulse OFF (milliseconds)"};
  // Existing hardware settings remain immediately visible, without an accordion.
  boolean connected=ble!=null&&ble.ready&&ble.revision>=0;
  boolean editable=connected&&licenseManager!=null&&licenseManager.hasValidLicense();
  // When offline, hardware settings cannot be edited. Show General Settings directly.
  if(connected)for(int i=0;i<keys.length;i++){
   final String key=keys[i],title=tr(names[i]);
   Button settingButton=new Button(this);settingButton.setAllCaps(false);
   settingButton.setText(title+"\n"+displayValue(key,ble!=null?valueOf(ble.config,key):"?"));
   settingButton.setEnabled(editable);settingButton.setAlpha(editable?1f:.45f);
   content.addView(settingButton,new LinearLayout.LayoutParams(-1,dp(62)));
   settingButton.setOnClickListener(vw->editSetting(key,title,settingButton));
  }
  // General Settings is the final, initially collapsed section.
  Button general=new Button(this);general.setAllCaps(false);
  general.setText((connected?"▸ ":"▾ ")+tr("General Settings"));
  LinearLayout.LayoutParams generalLp=new LinearLayout.LayoutParams(-1,dp(56));
  generalLp.topMargin=dp(18);
  content.addView(general,generalLp);
  LinearLayout generalContent=new LinearLayout(this);
  generalContent.setOrientation(LinearLayout.VERTICAL);
  generalContent.setPadding(dp(8),dp(6),dp(8),dp(8));
  generalContent.setVisibility(connected?View.GONE:View.VISIBLE);
  content.addView(generalContent,new LinearLayout.LayoutParams(-1,-2));
  general.setOnClickListener(vw->{
   boolean expanded=generalContent.getVisibility()!=View.VISIBLE;
   generalContent.setVisibility(expanded?View.VISIBLE:View.GONE);
   general.setText((expanded?"▾ ":"▸ ")+tr("General Settings"));
   if(expanded)scroll.post(()->scroll.smoothScrollTo(0,general.getTop()));
  });
  TextView langTitle=new TextView(this);langTitle.setText(tr("Language"));
  langTitle.setTextColor(Color.WHITE);langTitle.setTextSize(16);langTitle.setTypeface(null,Typeface.BOLD);
  LinearLayout.LayoutParams langTitleLp=new LinearLayout.LayoutParams(-1,-2);
  langTitleLp.topMargin=dp(18);
  generalContent.addView(langTitle,langTitleLp);
  Spinner languages=new Spinner(this);
  ArrayAdapter<String> languageAdapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,UiLanguage.OPTIONS);
  languages.setAdapter(languageAdapter);
  languages.setSelection(UiLanguage.selected(this));
  generalContent.addView(languages,new LinearLayout.LayoutParams(-1,dp(54)));
  languages.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
   @Override public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){
    if(position==UiLanguage.selected(MainActivity.this))return;
    UiLanguage.save(MainActivity.this,position);
    recreate();
   }
   @Override public void onNothingSelected(android.widget.AdapterView<?> parent){}
  });
  Button help=new Button(this);help.setAllCaps(false);help.setText("▸ "+tr("Help & information"));
  generalContent.addView(help,new LinearLayout.LayoutParams(-1,dp(52)));
  TextView info=new TextView(this);info.setTextColor(Color.LTGRAY);info.setTextSize(14);
  final String helpText="NORMAL: steady green. ALARM: saved orange effect and buzzer. ALL OFF is allowed only when NORMAL is confirmed.\n\nBluetooth: the owning phone reconnects automatically. The controller reserves ownership for up to 30 minutes after connection loss.\n\nAfter reinstalling the app, wait for the previous owner reservation to expire, or disconnect the previous app. The saved lighting settings remain in the controller.\n\nThe device is a visual and audible warning system, not a certified gas safety system.";
  info.setText(tr(helpText));
  info.setPadding(dp(10),dp(8),dp(10),dp(12));info.setVisibility(View.GONE);
  generalContent.addView(info);
  help.setOnClickListener(v->{
   boolean show=info.getVisibility()!=View.VISIBLE;
   info.setVisibility(show?View.VISIBLE:View.GONE);
   help.setText((show?"▾ ":"▸ ")+tr("Help & information"));
   if(show)scroll.post(()->scroll.smoothScrollTo(0,help.getTop()+generalContent.getTop()));
  });
  Button licensing=new Button(this);licensing.setAllCaps(false);
  licensing.setText("🔑 "+tr("License management")+" · HB-Electronics.nl");
  LinearLayout.LayoutParams licenseLp=new LinearLayout.LayoutParams(-1,dp(60));
  licenseLp.topMargin=dp(18);
  generalContent.addView(licensing,licenseLp);
  licensing.setOnClickListener(v->showLicenseManager());
  AlertDialog settingsDialog=new AlertDialog.Builder(this).setTitle("HP Gas Alert System · "+tr("Settings")+" v5.0")
   .setView(scroll).setPositiveButton(tr("Close"),null).create();
  settingsDialog.show();
  if(!connected)scroll.post(()->scroll.scrollTo(0,general.getTop()));
 }
 private void refreshLicenseLabel(){
  if(licenseStatus==null||licenseManager==null)return;
  String license=currentLicenseStatus();
  boolean valid=licenseManager.hasValidLicense();
  String label="🔑 "+license+" · "+tr("Tap for license management");
  if(!label.equals(lastLicenseBannerText)){
   licenseStatus.setText(label);
   lastLicenseBannerText=label;
  }
  int tone=valid?1:0;
  if(tone!=lastLicenseBannerColor){
   licenseStatus.setTextColor(valid?Color.rgb(190,255,203):Color.rgb(255,226,155));
   licenseStatus.setBackground(rounded(valid?Color.argb(90,20,99,48):Color.argb(235,130,28,34)));
   lastLicenseBannerColor=tone;
  }
  renderLicenseAccess();
 }
 private void checkLicenseOnStart(){
  if(licenseManager==null||licenseChecking)return;
  long now=SystemClock.elapsedRealtime();
  if(lastLicenseCheckAt>0&&now-lastLicenseCheckAt<LICENSE_CHECK_INTERVAL_MS)return;
  lastLicenseCheckAt=now;
  licenseChecking=true;
  if(!licenseManager.request("check","",(ok,message)->{
   licenseChecking=false;
   refreshLicenseLabel();
   render();
  })){
   licenseChecking=false;
  }
 }
 private boolean allowLicensedControl(){
  if(licenseManager!=null&&licenseManager.hasValidLicense())return true;
  showLicenseBlocked();
  return false;
 }
 private void openLicenseShop(){
  try{
   startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://hb-electronics.nl/")));
  }catch(Exception ex){
   Toast.makeText(this,tr("Unable to open the HB-Electronics webshop."),Toast.LENGTH_LONG).show();
  }
 }
 private void showLicenseBlocked(){
  if(isFinishing()||isDestroyed()||licenseManager==null||licenseManager.hasValidLicense())return;
  if(licenseBlockedDialog!=null&&licenseBlockedDialog.isShowing())return;
  String state=licenseManager.status();
  licenseWarningShownFor=state;
  String explanation=tr("Lamp control and settings are locked.")+"\n\n"
    +tr("Status:")+" "+currentLicenseStatus()+"\n\n"
    +tr("Activate a valid license, start an available trial, or purchase a license from HB-Electronics.");
  AlertDialog dialog=new AlertDialog.Builder(this)
   .setTitle(tr("License required · HP Gas Alert v5.0"))
   .setMessage(explanation)
   .setPositiveButton(tr("Activate license"),(d,w)->showLicenseManager())
   .setNeutralButton(tr("Buy license"),(d,w)->openLicenseShop())
   .setNegativeButton(tr("Close"),null).create();
  licenseBlockedDialog=dialog;
  dialog.setOnShowListener(d->{
   dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.rgb(72,189,125));
   dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.rgb(240,190,83));
  });
  dialog.show();
 }
 private void renderLicenseAccess(){
  if(licenseManager==null)return;
  if(licenseManager.hasValidLicense()){
   licenseWarningShownFor="";
   if(licenseBlockedDialog!=null&&licenseBlockedDialog.isShowing())licenseBlockedDialog.dismiss();
   return;
  }
  final String state=licenseManager.status();
  if(!state.equals(licenseWarningShownFor))
   h.post(this::showLicenseBlocked);
 }
 private void showLicenseManager(){
  HbLicenseManager manager=licenseManager;
  LinearLayout layout=new LinearLayout(this);
  layout.setOrientation(LinearLayout.VERTICAL);
  layout.setPadding(dp(22),dp(16),dp(22),dp(10));
  TextView header=new TextView(this);
  header.setText("HB-Electronics.nl · hb-gas-alert-v5");
  header.setTextColor(Color.LTGRAY);header.setTextSize(14);
  layout.addView(header);
  TextView statusText=new TextView(this);
  statusText.setText(currentLicenseStatus());statusText.setTextColor(Color.WHITE);
  statusText.setTextSize(17);statusText.setTypeface(null,Typeface.BOLD);
  statusText.setPadding(0,dp(12),0,dp(16));
  layout.addView(statusText);
  EditText licenseInput=new EditText(this);
  licenseInput.setSingleLine(true);
  licenseInput.setHint(tr("Enter license code"));
  licenseInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
  layout.addView(licenseInput);
  Button activate=new Button(this);activate.setAllCaps(false);activate.setText(tr("Activate license"));
  layout.addView(activate);
  Button trial=new Button(this);trial.setAllCaps(false);trial.setText(tr("Start trial"));
  layout.addView(trial);
  Button check=new Button(this);check.setAllCaps(false);check.setText(tr("Check license"));
  layout.addView(check);
  Button buy=new Button(this);buy.setAllCaps(false);buy.setText(tr("Buy license at HB-Electronics.nl"));
  layout.addView(buy);
  buy.setOnClickListener(v->openLicenseShop());
  final boolean[] running={false};
  final AlertDialog dialog=new AlertDialog.Builder(this)
   .setTitle(tr("License management · v5.0"))
   .setView(layout).setPositiveButton(tr("Close"),null).create();
  class Actions {
   void run(String action){
    if(running[0])return;
    String code=licenseInput.getText().toString().trim();
    if("activate".equals(action)&&code.isEmpty()){
     Toast.makeText(MainActivity.this,tr("Enter your license code first."),Toast.LENGTH_LONG).show();
     return;
    }
    running[0]=true;activate.setEnabled(false);trial.setEnabled(false);check.setEnabled(false);
    statusText.setText(tr("License server is checking…"));
    if(!manager.request(action,code,(ok,message)->{
     if(!dialog.isShowing())return;
     running[0]=false;activate.setEnabled(true);trial.setEnabled(true);check.setEnabled(true);
     statusText.setText(currentLicenseStatus());
     refreshLicenseLabel();
     render();
     showLicenseNotice(ok,message);
    })){
     running[0]=false;activate.setEnabled(true);trial.setEnabled(true);check.setEnabled(true);
     statusText.setText(currentLicenseStatus());
     Toast.makeText(MainActivity.this,tr("License check in progress."),Toast.LENGTH_LONG).show();
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
   statusText.setText(tr("Checking license status…")+"\n"+tr("Last known status:")+" "+UiLanguage.licenseStatus(this,lastKnown));
   if(!manager.request("check","",(ok,message)->{
    if(!dialog.isShowing())return;
    running[0]=false;activate.setEnabled(true);trial.setEnabled(true);check.setEnabled(true);
    statusText.setText(currentLicenseStatus());
    refreshLicenseLabel();
    render();
    if(!ok && manager.status().equals(lastKnown)){
     // A failed network request is not proof of revocation; make uncertainty clear.
     statusText.setText(tr("Last known status:")+" "+UiLanguage.licenseStatus(this,lastKnown)+"\n"+tr("Online check not confirmed."));
    }
   })){
    running[0]=false;activate.setEnabled(true);trial.setEnabled(true);check.setEnabled(true);
    statusText.setText(UiLanguage.licenseStatus(this,lastKnown));
   }
  }
 }
 private String valueOf(String cfg,String key){
  if(cfg==null)return "?";
  for(String item:cfg.split(";")){String[] pair=item.split("=",2);if(pair.length==2&&key.equals(pair[0]))return pair[1];}
  return "?";
 }
 private String displayValue(String key,String raw){
  if(raw==null||"?".equals(raw)||raw.isEmpty())return tr("Value unavailable");
  switch(key){
   case "greenBrightness":case "brightness":case "tint":case "afterglow":case "speed":
    return raw+"%";
   case "buzzerSeconds":return raw+" s";
   case "pulseOnMs":case "pulseOffMs":return raw+" ms";
   case "effect":return tr("Effect")+" "+raw+" / 40";
   case "leds":return raw+" "+tr("LEDs");
   case "layout":return "0".equals(raw)?tr("Single"):"1".equals(raw)?tr("Double mirrored"):tr("8 × 32 matrix");
   case "buzzerMode":return "0".equals(raw)?tr("Continuous"):tr("Pulsed");
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
  heading.setText(success?tr("✓ Saved successfully"):tr("Setting not saved"));
  heading.setTextSize(19);heading.setTypeface(null,Typeface.BOLD);
  heading.setTextColor(color);
  content.addView(heading);
  TextView bodyText=new TextView(this);
  bodyText.setText(UiLanguage.ble(this,body));
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
  heading.setText(success?"✓ "+tr("License checked"):revoked?tr("License revoked"):tr("License check failed"));
  heading.setTextSize(19);
  heading.setTypeface(null,Typeface.BOLD);
  heading.setTextColor(color);
  content.addView(heading);
  TextView explanation=new TextView(this);
  explanation.setText(UiLanguage.licenseMessage(this,message));
  explanation.setTextColor(Color.WHITE);
  explanation.setTextSize(15);
  explanation.setPadding(0,dp(12),0,dp(6));
  content.addView(explanation);
  AlertDialog popup=new AlertDialog.Builder(this)
   .setTitle("HB-Electronics · "+tr("License management"))
   .setView(content).setPositiveButton("OK",null).create();
  popup.setOnShowListener(d->popup.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color));
  popup.show();
 }
 private boolean saveSettingWithFeedback(String key,String raw,String title,Button origin){
  if(ble==null||!ble.ready){
   showSettingNotice(false,title,tr("Controller is not connected."));
   return false;
  }
  if(!"NORMAL".equals(ble.mode)){
   showSettingNotice(false,title,tr("Settings can only be changed in NORMAL (green). Reset the orange alarm first."));
   return false;
  }
  boolean accepted=ble.saveSetting(key,raw,(confirmed,detail)->runOnUiThread(()->{
   if(confirmed){
    origin.setText(title+"\n"+displayValue(key,raw));
    showSettingNotice(true,title,tr("The controller confirmed the setting in NORMAL (green).")+" "+displayValue(key,raw));
   }else{
    showSettingNotice(false,title,detail);
   }
  }));
  if(!accepted)showSettingNotice(false,title,tr("Controller busy or not ready to save. Please retry."));
  return accepted;
 }
 private void editSetting(String key,String title,Button origin){
  if(!allowLicensedControl()||ble==null||!ble.ready)return;
  if(!"NORMAL".equals(ble.mode)){
   showSettingNotice(false,title,tr("Settings are blocked while the orange alarm is active. Reset the alarm to NORMAL (green) before changing settings."));
   return;
  }
  if("leds".equals(key)){
   // The firmware stores the PHYSICAL TOTAL. In double-mirrored layout,
   // ask for LEDs per row and send twice that count (e.g. 37 -> 74).
   boolean doubleRow="1".equals(valueOf(ble.config,"layout"));
   int currentTotal=36;
   try{currentTotal=Integer.parseInt(valueOf(ble.config,"leds"));}catch(Exception ignored){}
   EditText count=new EditText(this);
   count.setSingleLine(true);
   count.setSelectAllOnFocus(true);
   count.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
   count.setText(String.valueOf(doubleRow?currentTotal/2:currentTotal));
   count.setSelectAllOnFocus(true);
   LinearLayout form=new LinearLayout(this);
   form.setOrientation(LinearLayout.VERTICAL);
   form.setPadding(dp(22),dp(8),dp(22),dp(2));
   TextView explanation=new TextView(this);
   explanation.setText(doubleRow
     ?tr("Enter the number of LEDs per row. The app calculates the physical total automatically. Example: 37 per row = 74 LEDs.")
     :tr("Enter the total number of physical LEDs (1–256)."));
   explanation.setTextSize(14);explanation.setTextColor(Color.LTGRAY);
   form.addView(explanation,new LinearLayout.LayoutParams(-1,-2));
   form.addView(count,new LinearLayout.LayoutParams(-1,dp(56)));
   AlertDialog inputDialog=new AlertDialog.Builder(this)
    .setTitle(doubleRow?tr("LEDs per row"):tr("Number of physical LEDs"))
    .setView(form)
    .setNegativeButton(tr("Cancel"),null)
    .setPositiveButton(tr("Save"),null).create();
   inputDialog.setOnShowListener(d->inputDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
    String value=count.getText().toString().trim();
    int perRowMax=doubleRow?128:256;
    int amount;
    try{amount=Integer.parseInt(value);}catch(NumberFormatException ex){amount=0;}
    if(amount<1||amount>perRowMax){
     count.setError(doubleRow?tr("Enter 1–128 LEDs per row."):tr("Enter 1–256 LEDs."));
     return;
    }
    int physicalTotal=doubleRow?amount*2:amount;
    if(saveSettingWithFeedback(key,String.valueOf(physicalTotal),title,origin))inputDialog.dismiss();
   }));
   inputDialog.show();
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
   hint.setText(tr("Selected value"));
   hint.setTextSize(13);
   hint.setTextColor(Color.LTGRAY);
   hint.setGravity(Gravity.CENTER_VERTICAL);
   valueRow.addView(hint,new LinearLayout.LayoutParams(0,dp(42),1f));
   TextView value=new TextView(this);
   value.setTextSize(23);
   value.setTypeface(null,Typeface.BOLD);
   value.setTextColor(Color.WHITE);
   value.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
   value.setContentDescription(tr("Selected value"));
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
    .setNegativeButton(tr("Cancel"),(d,w)->{
     if(pending[0]!=null)h.removeCallbacks(pending[0]);
     if(ble!=null)ble.command("CANCEL");
    })
    .setPositiveButton(tr("Save"),(d,w)->{
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
   if("layout".equals(key))labels=new String[]{tr("Single strip"),tr("Double mirrored strip"),tr("8 × 32 matrix")};
   else if("buzzerMode".equals(key))labels=new String[]{tr("Continuous"),tr("Pulsed")};
   else labels=new String[]{"GRB","RGB","RBG","GBR","BRG","BGR"};
   int current=0;try{current=Integer.parseInt(valueOf(ble.config,key));}catch(Exception ignored){}
   current=Math.max(0,Math.min(labels.length-1,current));
   new AlertDialog.Builder(this).setTitle(title).setSingleChoiceItems(labels,current,(d,which)->{
    d.dismiss();
    saveSettingWithFeedback(key,String.valueOf(which),title,origin);
   }).setNegativeButton(tr("Cancel"),null).show();
   return;
  }
  EditText edit=new EditText(this);edit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
  String initial=valueOf(ble.config,key);if(!"?".equals(initial))edit.setText(initial);
  new AlertDialog.Builder(this).setTitle(title).setView(edit)
   .setNegativeButton(tr("Cancel"),null).setPositiveButton(tr("Save"),(d,w)->{
    String val=edit.getText().toString().trim();
    if(!val.matches("[0-9]{1,5}")){Toast.makeText(this,tr("Enter a valid number"),Toast.LENGTH_LONG).show();return;}
    saveSettingWithFeedback(key,val,title,origin);
   }).show();
 }
 @Override protected void onStart(){
  super.onStart();
  bindService(new Intent(this,HpBleService.class),serviceConnection,BIND_AUTO_CREATE);
  h.post(refresher);
  checkLicenseOnStart();
}
 @Override protected void onStop(){
  super.onStop();h.removeCallbacks(refresher);stopScan();if(picker!=null)picker.dismiss();
  if(bound){ble.setListener(null);unbindService(serviceConnection);bound=false;ble=null;}
 }
}