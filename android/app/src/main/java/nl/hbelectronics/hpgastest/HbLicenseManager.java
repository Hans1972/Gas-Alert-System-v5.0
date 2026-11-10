package nl.hbelectronics.hpgastest;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** License status UI only: never interrupts BLE, active alarms or essential controls. */
public final class HbLicenseManager {
 public static final String PRODUCT="hb-gas-alert-v5";
 private static final String API="https://hb-electronics.nl/license-api.php";
 private static final String PUBLIC_KEY_B64=
  "MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAkpdcusAayflhHbEmnV0I"+
  "sDxUwfKmX+pFK5AJ7lTpzEfULNrKeRRlhXXE7KBb11bChn3Wvrvj5FTnA8d+IFAg"+
  "+wbtOopr2X1aIv+WIXgmwBEloSQiWrgKowA6yhQpQE6vzx1+GpgODeDzGczTgoti"+
  "P3Q8KmLEp8mMlo3m1HhP3oGBCQB0DZSXPhfSOjj8h6ZG37n+ag1min0NxvV1W/zo"+
  "PMOT8vwI1/SJHUUnCqzAzDX0Bymc2RkBPgvmk51CY3oLwn3rhXPsFOT0KJYU9RuG"+
  "UGJUIEvpwrVMrw/40WZTnJr8YN5z4n3QvCLCKBx2+Zsfw5T7iqjwJ10hiLmn8maJ"+
  "WYbbp21PLPyih5iLPzfaNNCQTeWKQADnuivWWiTX3c/f0+1eM3uJxnhyQBtdsFtx"+
  "qGv4jXhzjzdPHPPi9VLtN4ivqxSF8Dwbrerd3Qtwv8Y95j4Aj18yKH3OMxvufna4"+
  "7AKpT+D4TaWHju5Uajerwmu1570h0lUq/7k5w18AX3PVAgMBAAE=";
 public interface Callback { void complete(boolean ok,String detail); }
 private final SharedPreferences prefs;
 private final Handler main=new Handler(Looper.getMainLooper());
 private final String fingerprint,token;
 private volatile boolean pending=false;

 public HbLicenseManager(Context context){
  Context app=context.getApplicationContext();
  prefs=app.getSharedPreferences("gas_license_v5",Context.MODE_PRIVATE);
  String androidId=Settings.Secure.getString(app.getContentResolver(),Settings.Secure.ANDROID_ID);
  if(androidId==null)androidId="unknown";
  fingerprint=sha256("HB-GAS|"+app.getPackageName()+"|"+androidId);
  String saved=prefs.getString("token","");
  if(!saved.matches("[0-9a-f]{64}")){
   byte[] random=new byte[32];new SecureRandom().nextBytes(random);
   StringBuilder hex=new StringBuilder();
   for(byte b:random)hex.append(String.format(Locale.ROOT,"%02x",b&255));
   saved=hex.toString();prefs.edit().putString("token",saved).apply();
  }
  token=saved;
 }
 private static String sha256(String s){
  try{
   byte[] bytes=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
   StringBuilder hex=new StringBuilder();
   for(byte b:bytes)hex.append(String.format(Locale.ROOT,"%02x",b&255));
   return hex.toString();
  }catch(Exception e){throw new IllegalStateException(e);}
 }
 private JSONObject validated(String proof)throws Exception{
  String[] parts=proof.split("\\.",-1);
  if(parts.length!=2)throw new SecurityException("Invalid proof format");
  byte[] pub=Base64.decode(PUBLIC_KEY_B64,Base64.DEFAULT);
  PublicKey key=KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(pub));
  Signature verifier=Signature.getInstance("SHA256withRSA");
  verifier.initVerify(key);
  verifier.update(parts[0].getBytes(StandardCharsets.US_ASCII));
  byte[] sig=Base64.decode(parts[1],Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);
  if(!verifier.verify(sig))throw new SecurityException("Invalid license signature");
  JSONObject claims=new JSONObject(new String(Base64.decode(parts[0],Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING),StandardCharsets.UTF_8));
  long now=System.currentTimeMillis()/1000L;
  if(claims.optInt("v")!=1||!PRODUCT.equals(claims.optString("product"))
      ||!fingerprint.equals(claims.optString("device"))
      ||claims.optLong("issued")>now+300||claims.optLong("expires")<=now)
   throw new SecurityException("Expired or mismatched license");
  String type=claims.optString("type");
  if(!"trial".equals(type)&&!"paid".equals(type))throw new SecurityException("Invalid license category");
  return claims;
 }
 public String status(){
  if(prefs.getBoolean("revoked",false))return "Licentie ingetrokken";
  String proof=prefs.getString("proof","");
  if(proof.isEmpty())return "Nog niet geactiveerd";
  try{
   JSONObject claims=validated(proof);
   String date=new SimpleDateFormat("dd-MM-yyyy HH:mm",Locale.forLanguageTag("nl-NL"))
    .format(new Date(claims.getLong("expires")*1000L));
   return ("paid".equals(claims.getString("type"))?"Licentie actief":"Proefperiode actief")+" · geldig tot "+date;
  }catch(Exception e){return "Licentie verlopen of online controle nodig";}
 }
 public synchronized boolean request(String action,String licenseCode,Callback callback){
  if(pending||callback==null||!("trial".equals(action)||"check".equals(action)||"activate".equals(action)))return false;
  pending=true;
  new Thread(()->{
   boolean ok=false;String detail;
   HttpURLConnection connection=null;
   try{
    JSONObject request=new JSONObject();
    request.put("action",action);
    request.put("product",PRODUCT);
    request.put("device",fingerprint);
    request.put("token",token);
    request.put("name","HP Gas Alert System Android");
    request.put("version","5.0");
    if("activate".equals(action))request.put("code",licenseCode==null?"":licenseCode.trim());
    connection=(HttpURLConnection)new URL(API).openConnection();
    connection.setRequestMethod("POST");connection.setDoOutput(true);
    connection.setConnectTimeout(8000);connection.setReadTimeout(10000);
    connection.setRequestProperty("Content-Type","application/json; charset=UTF-8");
    try(OutputStream out=connection.getOutputStream()){
     out.write(request.toString().getBytes(StandardCharsets.UTF_8));
    }
    int responseCode=connection.getResponseCode();
    InputStream stream=responseCode<400?connection.getInputStream():connection.getErrorStream();
    if(stream==null)throw new IllegalStateException("Empty server response");
    ByteArrayOutputStream result=new ByteArrayOutputStream();
    byte[] buffer=new byte[4096];int count;
    try(InputStream input=stream){
     while((count=input.read(buffer))!=-1){
      if(result.size()+count>25000)throw new IllegalStateException("Server response too large");
      result.write(buffer,0,count);
     }
    }
    JSONObject response=new JSONObject(result.toString("UTF-8"));
    String status=response.optString("status");
    if(responseCode==200&&"ok".equals(status)){
     String proof=response.optString("proof");
     JSONObject claims=validated(proof);
     long serverTime=response.optLong("server_time");
     if(Math.abs(System.currentTimeMillis()/1000L-serverTime)>86400)
      throw new IllegalStateException("Phone clock differs from license server");
     prefs.edit().putString("proof",proof).putLong("last_check",serverTime).putBoolean("revoked",false).apply();
     ok=true;
     detail="Licentie gecontroleerd: "+("paid".equals(claims.optString("type"))?"geactiveerd":"proefperiode")+".";
    }else{
     if("revoked".equals(status)){
      // A server-confirmed revocation overrides any previously cached proof.
      prefs.edit().remove("proof").remove("last_check").putBoolean("revoked",true).apply();
      detail="Deze licentie is ingetrokken door HB-Electronics. De licentie is niet meer geldig. Neem contact op met HB-Electronics als dit volgens jou niet klopt.";
     }
     else if("unavailable".equals(status))detail="Product "+PRODUCT+" is nog niet actief op de HB-Electronics licentieserver.";
     else if("recovery_required".equals(status))detail="Heractivatie vereist na herinstallatie: voer de licentiecode opnieuw in.";
     else if("unknown".equals(status))detail="Deze installatie is nog niet aangemeld; start eerst de proefperiode.";
     else if("seat_limit".equals(status))detail="Maximum aantal apparaten bereikt.";
     else if("invalid_license".equals(status))detail="Ongeldige of verlopen licentiecode.";
     else detail="De licentie kon niet worden gecontroleerd. Controleer de licentiegegevens of probeer het opnieuw.";
    }
   }catch(Exception e){detail="Licentiecontrole niet gelukt: "+e.getMessage();}
   finally{if(connection!=null)connection.disconnect();}
   final boolean success=ok;final String resultMessage=detail;
   main.post(()->{pending=false;callback.complete(success,resultMessage);});
  },"GasAlert-License").start();
  return true;
 }
}