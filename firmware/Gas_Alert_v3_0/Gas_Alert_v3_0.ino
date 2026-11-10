/* Gas Alert System v3.0 BLE prototype — Seeed XIAO ESP32-C3.
 * Reuses the 40 animation algorithms and layout mappings from Nano v1.9.
 * NO WIFI. NO encoder. NO physical alarm input.
 * Arduino ESP32 core 3.x; Adafruit NeoPixel; built-in ESP32 BLE library.
 * IMPORTANT: this early implementation is for BENCH TESTS ONLY.
 * Owner token works across rotating BLE addresses, but unencrypted tokens are NOT secure.
 * BENCH TEST ONLY. NOT A CERTIFIED GAS SAFETY CONTROLLER.
 * D4=GPIO6 WS2812 data (via 74AHCT125 3.3V->5V level converter)
 * D5=GPIO7 ACTIVE HIGH buzzer DRIVER, NOT direct buzzer supply.
 */
#include <Arduino.h>
#include <Preferences.h>
#include <Adafruit_NeoPixel.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include <math.h>

// Explicit declarations prevent Arduino IDE auto-prototype errors.
struct Event;
static void processEvent(const Event &ev);
float band(float x,float pos,float width=0.12f);
float lightTrail(float x,float head,float width,float afterglow,bool reverse=false);
#ifndef D4
#define D4 6
#endif
#ifndef D5
#define D5 7
#endif
constexpr uint8_t DATA_PIN=D4, BUZZER_PIN=D5;
constexpr uint16_t MAX_LEDS=256;
constexpr uint32_t OWNER_TIMEOUT_MS=1800000UL;
struct Settings { uint16_t leds; uint8_t effect,brightness,tint; uint16_t speed; uint8_t colorMode,layout,afterglow; };
struct AlarmSettings { uint8_t greenBrightness,buzzerMode; uint16_t buzzerSeconds,pulseOnMs,pulseOffMs; };
Settings current={36,1,50,50,50,4,0,55};
AlarmSettings alarmCfg={35,0,8,300,300};
const neoPixelType COLOR_ORDERS[6]={NEO_GRB,NEO_RGB,NEO_RBG,NEO_GBR,NEO_BRG,NEO_BGR};
Adafruit_NeoPixel strip(36,DATA_PIN,NEO_BRG+NEO_KHZ800);
Preferences prefs;
enum Mode:uint8_t {OFF=0,NORMAL=1,ALARM=2};
Mode mode=NORMAL;
uint32_t revision=1,effectTime=0,lastFrame=0,lastTime=0,alarmStart=0;
uint16_t timeRemainder=0;
bool needsFrame=true,buzzerOn=false;
static bool valid(const Settings& s){return s.leds>=1&&s.leds<=MAX_LEDS&&s.effect>=1&&s.effect<=40&&s.brightness>=5&&s.brightness<=100&&s.tint<=100&&s.speed>=25&&s.speed<=200&&s.colorMode<6&&s.layout<=2&&s.afterglow<=100&&(s.layout!=1||(s.leds%2)==0)&&(s.layout!=2||s.leds==256);}
static bool validAlarm(const AlarmSettings& a){return a.greenBrightness<=100&&a.buzzerMode<=1&&a.buzzerSeconds<=120&&a.pulseOnMs>=50&&a.pulseOnMs<=5000&&a.pulseOffMs>=50&&a.pulseOffMs<=5000;}
static void buzzer(bool on){if(on!=buzzerOn){buzzerOn=on;digitalWrite(BUZZER_PIN,on?HIGH:LOW);}}
static void store(){prefs.putBytes("effect",&current,sizeof(current));prefs.putBytes("alarm",&alarmCfg,sizeof(alarmCfg));prefs.putUChar("mode",mode);}
static void load(){prefs.begin("gas-v3",false);Settings s;AlarmSettings a;
 if(prefs.getBytesLength("effect")==sizeof(s)&&prefs.getBytes("effect",&s,sizeof(s))==sizeof(s)&&valid(s))current=s;
 if(prefs.getBytesLength("alarm")==sizeof(a)&&prefs.getBytes("alarm",&a,sizeof(a))==sizeof(a)&&validAlarm(a))alarmCfg=a;
 // If reboot while alarm, restore warning LIGHT, with buzzer re-triggered on reboot.
 uint8_t stored=prefs.getUChar("mode",NORMAL);mode=stored<=ALARM?(Mode)stored:NORMAL;
}
static void applyStrip(){strip.clear();strip.show();strip.updateLength(current.leds);strip.updateType(COLOR_ORDERS[current.colorMode]+NEO_KHZ800);strip.clear();strip.show();needsFrame=true;}
float unitWave(float x){return (1.0f-cosf(x*6.2831853f))*0.5f;}
float clamp01(float x){return x<0?0:x>1?1:x;}
float band(float x,float pos,float width){return clamp01(1.0f-fabsf(x-pos)/width);}
float wrappedBand(float x,float pos,float width){float d=fabsf(x-pos);if(d>0.5f)d=1.0f-d;return clamp01(1.0f-d/width);}
float squareValue(float x){return x*x;}
// Positie op 360 graden: eindpunt 1.0 is hetzelfde als 0.0.
float wrapPhase(float v){while(v>=1.0f)v-=1.0f;while(v<0.0f)v+=1.0f;return v;}
// Straal met helder centrum en zachte staart, softwarematig gelijke oranjetint.
// Naadloos rondom voor paneel dat tot een cilinder is gebogen.
float lightTrail(float x,float head,float width,float afterglow,bool reverse){
 float d=reverse?wrapPhase(x-head):wrapPhase(head-x);
 float core=wrappedBand(x,head,width);
 if(afterglow<=0.001f)return core>=.20f?1.0f:0.0f;
 float tailWidth=.045f+afterglow*.45f;
 float tail=clamp01(1.0f-d/tailWidth)*.86f;
 return fmaxf(core,tail);
}
float effectLevel(uint8_t effect,uint16_t i,uint16_t count,uint32_t t){
 float x=count<=1?0.5f:(float)i/(count-1);
 float a=(t%2400)/2400.0f,p=unitWave((t%3200)/3200.0f);
 // Alle patronen gebruiken niveau 0 als volledig UIT (geen achtergrondlicht).
 switch(effect){
 case 1:return unitWave((t%2000)/2000.0f);
 case 2:return band(x,a*1.3f-.15f);
 case 3:{float w=unitWave((t%2000)/2000.0f);return (i<count/2?w:1-w);}
 case 4:return band(x,1.15f-a*1.3f);
 case 5:return band(x,p);
 case 6:{float d=(t%2600)/2600.0f*.65f;return fmaxf(band(x,.5f-d),band(x,.5f+d));}
 case 7:{float d=(1-(t%2600)/2600.0f)*.65f;return fmaxf(band(x,.5f-d),band(x,.5f+d));}
 case 8:return unitWave(x-(t%3000)/3000.0f);
 case 9:return unitWave(2*x-(t%3000)/3000.0f);
 case 10:{float w=unitWave((t%2400)/2400.0f);uint8_t block= (uint32_t)i*8/count;return (block%2?w:1-w);}
 case 11:return clamp01((unitWave((t%4000)/4000.0f)*1.2f-.1f-x+.08f)/.16f);
 case 12:{float u=t%3000;return fmaxf(expf(-squareValue((u-500)/200)),.8f*expf(-squareValue((u-1100)/200)));}
 case 13:return fmaxf(band(x,p,.09f),band(x,1-p,.09f));
 case 14:return fmaxf(wrappedBand(x,a,.08f),fmaxf(wrappedBand(x,fmodf(a+1.0f/3,1),.08f),wrappedBand(x,fmodf(a+2.0f/3,1),.08f)));
 case 15:return clamp01((unitWave((t%4000)/4000.0f)*.62f-fabsf(x-.5f))/.1f);
 case 16:{uint8_t part=(uint32_t)i*4/count;float u=fmodf((t%3200)/3200.0f-part/4.0f+1,1);return (u<.35f?squareValue(sinf(u/.35f*3.14159265f)):0);}
 case 17:{const float v[4]={0,.5f,1,.5f};float k=(t%3200)/800.0f;uint8_t j=k;float z=clamp01((k-j)/.25f);float w=unitWave(z*.5f);return v[j]*(1-w)+v[(j+1)%4]*w;}
 case 18:{float u=(t%3000)/3000.0f,d=u*.7f;return fmaxf(expf(-squareValue((x-.5f)/.15f))*expf(-squareValue(u/.18f)),fmaxf(band(x,.5f-d,.09f),band(x,.5f+d,.09f)));}
 case 19:{float u=(t%4800)/2400.0f;return band(x,u<1?u*1.3f-.15f:1.15f-(u-1)*1.3f);}
 case 20:return fmaxf(wrappedBand(x,a,.14f),wrappedBand(x,fmodf(a+.5f,1),.14f));

 case 21:{ // 1 continu roterende straal, overgang rechts -> links is naadloos.
   float h=(t%2400)/2400.0f;return lightTrail((float)i/count,h,.075f,current.afterglow/100.0f);
 }
 case 22:{ // twee koppen precies tegenover elkaar
   float h=(t%3200)/3200.0f;
   return fmaxf(lightTrail((float)i/count,h,.065f,current.afterglow/100.0f),
                lightTrail((float)i/count,wrapPhase(h+.5f),.065f,current.afterglow/100.0f));
 }
 case 23:{ // smalle straal, relatief korte staart
   float h=(t%1600)/1600.0f;return lightTrail((float)i/count,h,.035f,current.afterglow/250.0f);
 }
 case 24:{ // omgekeerde draairichting met staart aan andere kant
   float h=1.0f-(t%2400)/2400.0f;return lightTrail((float)i/count,h,.065f,current.afterglow/100.0f,true);
 }
 case 25:{ // wisselt elke 2.4 s van richting; bij wissel keert ook de staart
   float q=(t%4800)/2400.0f;bool rev=q>=1.0f;
   float h=rev?2.0f-q:q;return lightTrail((float)i/count,h,.075f,current.afterglow/100.0f,rev);
 }
 case 26:{ // twee korte flitsen en pauze, maximaal 2 per 2 s
   uint32_t q=t%2000;return (q<110 || (q>=320 && q<430))?1.0f:0.0f;
 }
 case 27:{ // drie korte flitsen en lange pauze
   uint32_t q=t%2400;return (q<90 || (q>=300&&q<390) || (q>=600&&q<690))?1.0f:0.0f;
 }
 case 28:{ // links/rechts, afwisselend flitsende helften
   uint32_t q=t%2400;uint8_t half=(q/600)%2;return ((q%600)<130 && ((i<count/2)?0:1)==half)?1.0f:0.0f;
 }
 case 29:{ // vier blokken flitsen om beurten
   uint32_t q=t%3200;uint8_t quarter=(q/800)%4;
   return ((q%800)<140 && (uint32_t)i*4/count==quarter)?1.0f:0.0f;
 }
 case 30:{ // rustige full-frame flits eens per 1000ms
   return (t%1000)<100?1.0f:0.0f;
 }
 case 31:{ // camera: volle flits en gelijkmatig afnemende nagloed
   uint32_t q=t%2400;
   if(q<90)return 1.0f;
   if(q<620)return clamp01(1.0f-(q-90)/530.0f)*.75f;
   return 0.0f;
 }
 case 32:{ // dubbele hartslag met variabele tweede puls
   uint32_t q=t%2400;
   if(q<270)return unitWave(q/270.0f);
   if(q>=420&&q<760)return .75f*unitWave((q-420)/340.0f);
   return 0.0f;
 }
 case 33:{ // radar: scherpe kop met extra lange staart
   float h=(t%3200)/3200.0f;
   return lightTrail((float)i/count,h,.026f,current.afterglow/85.0f);
 }
 case 34:{ // drie gelijk verdeelde, harde rondlopende lichtpunten
   float x2=(float)i/count,h=(t%2400)/2400.0f;
   for(uint8_t k=0;k<3;k++)if(wrappedBand(x2,wrapPhase(h+k/3.0f),.045f)>.45f)return 1.0f;
   return 0.0f;
 }
 case 35:{ // vijf snellere rondlopende lichtpunten
   float x2=(float)i/count,h=(t%1600)/1600.0f;
   for(uint8_t k=0;k<5;k++)if(wrappedBand(x2,wrapPhase(h+k/5.0f),.028f)>.4f)return 1.0f;
   return 0.0f;
 }
 case 36:{ // heen-en-weer langs lengte met naijl in de bewegingsrichting
   float q=(t%4000)/2000.0f;bool rev=q>=1.0f;
   float h=rev?2.0f-q:q;
   float d=rev?(x-h):(h-x); // niet rondom, stopt bij de uiteinden
   float length=.1f+.40f*current.afterglow/100.0f;
   if(d<0)return band(x,h,.08f);
   return fmaxf(band(x,h,.08f),clamp01(1.0f-d/length)*.8f);
 }
 case 37:{ // komeet: lange ronde staart, kleine intense kop
   float h=(t%4000)/4000.0f;
   return lightTrail((float)i/count,h,.03f,current.afterglow/85.0f);
 }
 case 38:{ // lopende blokken van 4 posities per 16; in een cirkel doorlopend
   uint8_t logical=(uint8_t)(((uint32_t)i*32/count+(t%3200)/100)%16);
   return logical<4?1.0f:0.0f;
 }
 case 39:{ // een kwart tegelijk aan, rondgaand
   uint8_t quadrant=(uint32_t)i*4/count;
   return quadrant==(uint8_t)((t%3200)/800)?1.0f:0.0f;
 }
 case 40:{ // zwaailicht plus een enkele korte krachtige alarmflits
   uint32_t q=t%4000;
   if(q>=3200 && q<3310)return 1.0f;
   float h=q/4000.0f;
   return lightTrail((float)i/count,h,.065f,current.afterglow/100.0f);
 }
 }return 0;
}

// v1.8: alleen bewegende lichtbanden met zachte randen en naijl-/golfpatronen
// mogen tussen naburige leds in helderheid verschillen. De kleur (tint)
// blijft ook in deze effecten gelijk; alleen de lichtsterkte wordt geschaald.
bool hasTrailingFade(uint8_t effect){
  switch(effect){
    case 2: case 4: case 5: case 6: case 7:
    case 11: case 13: case 14: case 15:
    case 18: case 19: case 20:
    case 21: case 22: case 23: case 24: case 25:
    case 33: case 36: case 37: case 40:
      return true;
    default:
      return false;
  }
}

// Bij de overige effecten zijn pixels ofwel UIT ofwel exact dezelfde
// ingeschakelde kleur. Een volledige puls (1,12,17) mag wel als groep
// op- en afdimmen, omdat alle LEDs dan op hetzelfde moment gelijk blijven.
// Kleine helderheden bij naijlen worden helemaal uitgezet: bij 8-bit PWM
// gaan anders de R/G-waarden sterk afronden en lijken leds roze of geel.
float outputLevel(uint8_t effect, float level){
  level=clamp01(level);
  if(effect==1 || effect==12 || effect==17 || effect==31 || effect==32){
    return level<0.12f ? 0.0f : level;
  }
  if(hasTrailingFade(effect)){
    return level<0.12f ? 0.0f : level;
  }
  return level>=0.5f ? 1.0f : 0.0f;
}

// Altijd dezelfde basiskleur; alleen bij puls of naijl worden RGB-kanalen
// in dezelfde verhouding gedimd. Bij lage waarden clamp om roze puntjes
// door afronding te vermijden. Een hardwareverschil kan de firmware niet corrigeren.
uint32_t logicalPixelColor(uint16_t pos,uint16_t logicalCount){
  const float raw=effectLevel(current.effect,pos,logicalCount,effectTime);
  const float level=outputLevel(current.effect,raw);
  if(level<=0.0f)return 0;
  const uint8_t baseR=(uint16_t)255*current.brightness/100;
  const uint8_t baseG=(uint16_t)(65+current.tint*85/100)*current.brightness/100;
  uint8_t r=(uint8_t)(baseR*level+0.5f);
  uint8_t g=(uint8_t)(baseG*level+0.5f);
  if(g<3)return 0;
  return strip.Color(r,g,0);
}

// Een centrale mapping voor ALLE effecten en diagnostiek.
// Enkele strip: 1-op-1. Dubbel: fysieke LED 1 naast de laatste LED.
// 8x32 paneel: iedere horizontale positie is een hele verticale kolom.
uint16_t logicalCount(){
  return current.layout==1 ? current.leds/2 : (current.layout==2 ? 32 : current.leds);
}
void setLogicalPixel(uint16_t pos,uint32_t color){
  if(pos>=logicalCount())return;
  if(current.layout==0){
    strip.setPixelColor(pos,color);
  }else if(current.layout==1){
    strip.setPixelColor(pos,color);
    strip.setPixelColor(current.leds-1-pos,color);
  }else{
    for(uint8_t y=0;y<8;y++){
      uint16_t index=pos*8 + ((pos&1)?(7-y):y);
      strip.setPixelColor(index,color);
    }
  }
}



static void renderMode(uint32_t now){
 if(mode==OFF){if(needsFrame){strip.clear();strip.show();needsFrame=false;}return;}
 if(mode==NORMAL){if(needsFrame){uint8_t g=255*alarmCfg.greenBrightness/100;strip.fill(strip.Color(0,g,0));strip.show();needsFrame=false;}return;}
 uint16_t n=logicalCount();for(uint16_t pos=0;pos<n;pos++)setLogicalPixel(pos,logicalPixelColor(pos,n));strip.show();
}
static void updateBuzzer(uint32_t now){
 if(mode!=ALARM){buzzer(false);return;}
 uint32_t age=now-alarmStart;
 if(age>=uint32_t(alarmCfg.buzzerSeconds)*1000UL){buzzer(false);return;}
 if(alarmCfg.buzzerMode==0){buzzer(true);return;}
 uint32_t cycle=uint32_t(alarmCfg.pulseOnMs)+alarmCfg.pulseOffMs;
 buzzer((age%cycle)<alarmCfg.pulseOnMs);
}
// BLE protocol v3.0 PROTOTYPE (not compatible with v1.4 app):
// WRITE CMD UUID: "ALARM", "RESET", "OFF", "ON", or "SET key=value".
// READ/NOTIFY STATUS UUID: "NORMAL|revision", "ALARM|revision", "OFF|revision".
// SETTINGS READ UUID: semicolon-separated current settings.
// NO claim of authenticated ownership: lock is a functional demo only.
static const char* SVC="b8f23000-6a9b-4a57-9b80-43db7c4f1000";
static const char* CMD="b8f23001-6a9b-4a57-9b80-43db7c4f1000";
static const char* STAT="b8f23002-6a9b-4a57-9b80-43db7c4f1000";
static const char* CFG="b8f23003-6a9b-4a57-9b80-43db7c4f1000";
BLEServer* bleServer=nullptr;
BLECharacteristic *statusChar=nullptr,*configChar=nullptr;
QueueHandle_t bleQueue=nullptr;
struct Event {uint8_t kind;char text[72];char address[18];}; // kind 1 connect,2 disconnect,3 command
bool clientConnected=false,ownerPresent=false,linkConnected=false,releaseAcknowledged=false;
char ownerToken[33]={0};
uint32_t ownerDisconnectedAt=0,linkConnectedAt=0,lastHeartbeat=0;
static void cancelPreview();
String serialCommand;
static void clearOwnerFromUsb(){
  // Requires physical USB serial access. Never expose an owner-reset BLE command.
  prefs.remove("owner");
  ownerPresent=false;ownerToken[0]=0;
  ownerDisconnectedAt=millis();
  clientConnected=false;
  cancelPreview();
  Serial.println("Owner reservation cleared over USB; connect and claim with current v3.0 app");
  if(linkConnected&&bleServer)bleServer->disconnect(bleServer->getConnId());
}
static void pollUsbCommands(){
 while(Serial.available()){
  char c=(char)Serial.read();
  if(c=='\r'||c=='\n'){
   serialCommand.trim();
   if(serialCommand=="OWNER RESET")clearOwnerFromUsb();
   else if(serialCommand=="OWNER STATUS"){
    Serial.printf("Owner reserved: %s; BLE connected: %s; phone authorized: %s\n",
     ownerPresent?"yes":"no",linkConnected?"yes":"no",clientConnected?"yes":"no");
   }else if(serialCommand.length())Serial.println("Unknown USB command. Use OWNER STATUS or OWNER RESET.");
   serialCommand="";
  }else if(serialCommand.length()<64 && c>=32 && c<=126)serialCommand+=c;
 }
}

static bool validToken(const String& t){
 if(t.length()!=32)return false;
 for(size_t i=0;i<32;i++){char c=t[i];if(!((c>='0'&&c<='9')||(c>='a'&&c<='f')))return false;}
 return true;
}
static void statusNotify(){if(!statusChar)return;
 String m=releaseAcknowledged?"RELEASED":clientConnected?(mode==NORMAL?"NORMAL":mode==ALARM?"ALARM":"OFF"):"LOCKED";
 m+="|"+String(revision);
 statusChar->setValue(m.c_str());if(linkConnected)statusChar->notify();
}
static void settingsUpdate(){if(!configChar)return;
 String s="leds="+String(current.leds)+";effect="+String(current.effect)+";brightness="+String(current.brightness)+";tint="+String(current.tint);
 s+=";speed="+String(current.speed)+";colorMode="+String(current.colorMode)+";layout="+String(current.layout);
 s+=";afterglow="+String(current.afterglow)+";greenBrightness="+String(alarmCfg.greenBrightness);
 s+=";buzzerMode="+String(alarmCfg.buzzerMode)+";buzzerSeconds="+String(alarmCfg.buzzerSeconds);
 s+=";pulseOnMs="+String(alarmCfg.pulseOnMs)+";pulseOffMs="+String(alarmCfg.pulseOffMs);
 configChar->setValue(s.c_str());
}
// Preview changes affect RAM and LEDs only; NVS is written on SET.
Settings previewSettings;
AlarmSettings previewAlarm;
bool previewActive=false;
static void cancelPreview(){
 if(!previewActive)return;
 current=previewSettings;alarmCfg=previewAlarm;previewActive=false;
 applyStrip();revision++;settingsUpdate();statusNotify();
}
static bool setValue(const String &key,unsigned long v,bool persist=true){
 Settings s=current;AlarmSettings a=alarmCfg;
 if(key=="leds")s.leds=v;
 else if(key=="effect")s.effect=v;
 else if(key=="brightness")s.brightness=v;
 else if(key=="tint")s.tint=v;
 else if(key=="speed")s.speed=v;
 else if(key=="colorMode")s.colorMode=v;
 else if(key=="layout")s.layout=v;
 else if(key=="afterglow")s.afterglow=v;
 else if(key=="greenBrightness")a.greenBrightness=v;
 else if(key=="buzzerMode")a.buzzerMode=v;
 else if(key=="buzzerSeconds")a.buzzerSeconds=v;
 else if(key=="pulseOnMs")a.pulseOnMs=v;
 else if(key=="pulseOffMs")a.pulseOffMs=v;
 else return false;
 if(!valid(s)||!validAlarm(a)||mode==ALARM)return false;
 if(!persist&&!previewActive){previewSettings=current;previewAlarm=alarmCfg;previewActive=true;}
 const bool structural=(s.leds!=current.leds||s.layout!=current.layout||s.colorMode!=current.colorMode);
 current=s;alarmCfg=a;
 if(structural)applyStrip();
 else needsFrame=true;
 if(persist){store();previewActive=false;}
 revision++;settingsUpdate();statusNotify();
 return true;
}
static void doCommand(const char* text){
 String cmd(text);cmd.trim();Mode target=mode;
 if(cmd=="ALARM"&&mode==NORMAL)target=ALARM;
 else if(cmd=="RESET"&&mode==ALARM)target=NORMAL;
 else if(cmd=="OFF"&&mode==NORMAL)target=OFF;
 else if(cmd=="ON"&&mode==OFF)target=NORMAL;
 else if(cmd=="CANCEL"){
  cancelPreview();statusNotify();return;
 }else if(cmd=="RELEASE"){
  // Only an already-authorized owner can reach doCommand().
  // Keep BLE alive until the Android app has READ the RELEASED acknowledgement.
  cancelPreview();
  prefs.remove("owner");
  ownerPresent=false;ownerToken[0]=0;ownerDisconnectedAt=0;
  releaseAcknowledged=true;
  clientConnected=false; // No further control commands can execute on this now-unowned link.
  revision++;
  Serial.println("Owner intentionally released by authorized phone; reservation removed");
  statusNotify();return;
 }else if(cmd.startsWith("SET ")||cmd.startsWith("PREVIEW ")){
  bool persist=cmd.startsWith("SET ");
  String pair=cmd.substring(persist?4:8);
  int sep=pair.indexOf('=');
  if(sep>0){
   String key=pair.substring(0,sep),value=pair.substring(sep+1);
   bool numeric=value.length()>0;
   for(size_t i=0;i<value.length();i++)if(!isDigit(value[i]))numeric=false;
   if(numeric){
    unsigned long v=strtoul(value.c_str(),nullptr,10);
    if(setValue(key,v,persist))return;
   }
  }
  Serial.println("Rejected settings command");statusNotify();return;
 }else {Serial.println("Rejected mode transition");statusNotify();return;}
 mode=target;revision++;needsFrame=true;if(mode==ALARM){alarmStart=millis();effectTime=0;}else buzzer(false);
 store();renderMode(millis());updateBuzzer(millis());statusNotify();
}
static void enqueue(uint8_t kind,const String& address,const String& value=""){
 if(!bleQueue)return;Event ev={};ev.kind=kind;
 strncpy(ev.address,address.c_str(),sizeof(ev.address)-1);
 strncpy(ev.text,value.c_str(),sizeof(ev.text)-1);
 xQueueSend(bleQueue,&ev,0);
}
class ServerCallbacks:public BLEServerCallbacks {
public:
 void onConnect(BLEServer*) override {enqueue(1,"");}
 void onDisconnect(BLEServer*) override {enqueue(2,"");BLEDevice::startAdvertising();}
};
class CommandCallbacks:public BLECharacteristicCallbacks {
 void onWrite(BLECharacteristic* c) override {
  String value=c->getValue();if(value.isEmpty()||value.length()>=72)return;
  enqueue(3,"",value);
 }
};
// Auto-expire abandoned owner reservations after the original 30-minute hold.
// Unauthenticated connections must NOT prolong the hold or stop its expiry.
static void expireAbandonedOwner(uint32_t now){
 if(!ownerPresent||clientConnected)return;
 if(uint32_t(now-ownerDisconnectedAt)<OWNER_TIMEOUT_MS)return;
 ownerPresent=false;
 ownerToken[0]=0;
 ownerDisconnectedAt=0;
 prefs.remove("owner");
 Serial.println("Old owner automatically expired after 30 minutes; new installation may claim");
}
static void processEvent(const Event &ev){
 uint32_t now=millis();
 if(ev.kind==1){
  linkConnected=true;clientConnected=false;releaseAcknowledged=false;linkConnectedAt=now;
  Serial.println("BLE connected - awaiting AUTH");statusNotify();
 }else if(ev.kind==2){
  if(clientConnected){clientConnected=false;ownerDisconnectedAt=now;Serial.println("Owner disconnected; hold started");}
  cancelPreview();
  releaseAcknowledged=false;linkConnected=false;statusNotify();
 }else if(ev.kind==3){
  if(!linkConnected)return;
  String cmd(ev.text);cmd.trim();
  if(cmd.startsWith("AUTH ")){
   String token=cmd.substring(5);token.toLowerCase();
   if(!validToken(token)){Serial.println("Invalid AUTH");statusNotify();return;}
   expireAbandonedOwner(now);
   if(!ownerPresent){token.toCharArray(ownerToken,sizeof(ownerToken));ownerPresent=true;prefs.putString("owner",token);Serial.println("First owner claimed");}
   if(token.equals(ownerToken)){clientConnected=true;ownerDisconnectedAt=0;Serial.println("Owner token recognized");statusNotify();settingsUpdate();}
   else{Serial.println("BLE owner reserved for a different installation; automatic release after 30 minutes");statusNotify();if(bleServer)bleServer->disconnect(bleServer->getConnId());}
  }else if(clientConnected)doCommand(ev.text);
  else{Serial.println("Command refused: AUTH required");statusNotify();}
 }
}
void setup(){
 digitalWrite(BUZZER_PIN,LOW);pinMode(BUZZER_PIN,OUTPUT);
 Serial.begin(115200);
 strip.begin();strip.clear();strip.show();load();applyStrip();
 String prior=prefs.getString("owner","");
 if(validToken(prior)){prior.toCharArray(ownerToken,sizeof(ownerToken));ownerPresent=true;ownerDisconnectedAt=millis();Serial.println("Saved owner restored after reboot");}
 if(mode==ALARM)alarmStart=millis();renderMode(millis());updateBuzzer(millis());
 bleQueue=xQueueCreate(12,sizeof(Event));
 if(!bleQueue){Serial.println("FATAL: BLE queue allocation failed");while(true)delay(1000);}
 BLEDevice::init("HP Gas Alert System v3.0");
 bleServer=BLEDevice::createServer();bleServer->setCallbacks(new ServerCallbacks());
 BLEService* service=bleServer->createService(SVC);
 BLECharacteristic* cmd=service->createCharacteristic(CMD,BLECharacteristic::PROPERTY_WRITE);
 cmd->setCallbacks(new CommandCallbacks());
 statusChar=service->createCharacteristic(STAT,BLECharacteristic::PROPERTY_READ|BLECharacteristic::PROPERTY_NOTIFY);
 statusChar->addDescriptor(new BLE2902());
 configChar=service->createCharacteristic(CFG,BLECharacteristic::PROPERTY_READ);
 statusNotify();settingsUpdate();service->start();
 BLEAdvertising *advert=BLEDevice::getAdvertising();advert->addServiceUUID(SVC);advert->setScanResponse(true);
 BLEDevice::startAdvertising();lastTime=millis();
 Serial.println("BLE v3.0 FIX9 - automatic 30-minute owner expiry; BENCH TEST ONLY");
 Serial.println("Owner automatically releases after 30 minutes without the owning phone. USB recovery is optional.");
}
void loop(){
 pollUsbCommands();
 Event ev;while(xQueueReceive(bleQueue,&ev,0)==pdTRUE)processEvent(ev);
 uint32_t now=millis();
 expireAbandonedOwner(now);
 if(linkConnected&&!clientConnected&&uint32_t(now-linkConnectedAt)>=8000UL){
  Serial.println("AUTH timeout");linkConnectedAt=now;
  if(bleServer)bleServer->disconnect(bleServer->getConnId());
 }
 if(linkConnected&&uint32_t(now-lastHeartbeat)>=3000UL){lastHeartbeat=now;statusNotify();}
 updateBuzzer(now);
 uint32_t dt=now-lastTime;lastTime=now;uint32_t scaled=dt*current.speed+timeRemainder;
 effectTime=(effectTime+scaled/100)%624000UL;timeRemainder=scaled%100;
 if(needsFrame||(mode==ALARM&&now-lastFrame>=33)){lastFrame=now;renderMode(now);}
}