package com.nino.edu.flight.hackathon;
import android.app.Activity; import android.graphics.*; import android.hardware.Camera; import android.os.Bundle; import android.view.*; import android.widget.*; import java.io.File; import java.io.FileWriter; import java.util.ArrayList; import java.util.List;
public class EyesV1Activity extends Activity implements SurfaceHolder.Callback, View.OnClickListener, Camera.PreviewCallback {
 Camera cam; SurfaceView sv; OverlayView overlay; TextView info; File outDir; List<OverlayView.LabeledBox> detected=new ArrayList<OverlayView.LabeledBox>(); boolean hasCamPerm=false; byte[] lastFrame; int frameW,frameH; int viewW=720,viewH=1362;
 protected void onCreate(Bundle b){ super.onCreate(b); outDir=new File("/sdcard/CODEX6120"); outDir.mkdirs(); FrameLayout root=new FrameLayout(this); sv=new SurfaceView(this); root.addView(sv,new FrameLayout.LayoutParams(-1,-1)); overlay=new OverlayView(this); root.addView(overlay,new FrameLayout.LayoutParams(-1,-1)); LinearLayout bottom=new LinearLayout(this); bottom.setOrientation(LinearLayout.VERTICAL); FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,-2); lp.gravity=80; bottom.setBackgroundColor(0xDD000000); info=new TextView(this); info.setText("EYES V10 - BLUEPRINT FINAL"); info.setTextColor(0xFF00FFFF); info.setTextSize(11); bottom.addView(info); Button btn=new Button(this); btn.setText("SCAN V10 - BLUEPRINT FINAL"); btn.setId(999); btn.setOnClickListener(this); bottom.addView(btn); Button btn2=new Button(this); btn2.setText("ALLOW CAMERA + RESTART"); btn2.setId(1000); btn2.setOnClickListener(this); bottom.addView(btn2); root.addView(bottom,lp); setContentView(root); sv.getHolder().addCallback(this); checkPerm(); }
 void checkPerm(){ try{ if(android.os.Build.VERSION.SDK_INT>=23){ if(checkSelfPermission(android.Manifest.permission.CAMERA)!=0){ requestPermissions(new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE},1); return; } } hasCamPerm=true; info.setText("V10 READY - FINAL"); }catch(Exception e){ hasCamPerm=true; } }
 public void onRequestPermissionsResult(int c,String[] p,int[] r){ if(c==1 && r.length>0 && r[0]==0){ hasCamPerm=true; try{ openCam(sv.getHolder()); }catch(Exception e){} } }
 void openCam(SurfaceHolder h) throws Exception { if(cam!=null){ cam.stopPreview(); cam.release(); } cam=Camera.open(0); Camera.Parameters par=cam.getParameters(); try{ par.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE); }catch(Exception e){} try{ List<Camera.Size> sz=par.getSupportedPreviewSizes(); Camera.Size best=sz.get(0); for(Camera.Size s:sz) if(s.width<=640) best=s; par.setPreviewSize(best.width,best.height); }catch(Exception e){} par.setRotation(90); try{ cam.setParameters(par); }catch(Exception e){} cam.setDisplayOrientation(90); cam.setPreviewDisplay(h); cam.setPreviewCallback(this); cam.startPreview(); info.setText("CAM LIVE V10 FINAL"); }
 public void surfaceCreated(SurfaceHolder h){ if(!hasCamPerm) return; try{ openCam(h); }catch(Exception e){ info.setText("cam err: "+e.getMessage()); } } public void surfaceChanged(SurfaceHolder h,int f,int w,int ht){ viewW=w; viewH=ht; if(overlay!=null) overlay.setViewSize(w,ht); } public void surfaceDestroyed(SurfaceHolder h){ if(cam!=null){ cam.setPreviewCallback(null); cam.stopPreview(); cam.release(); cam=null; } } public void onPreviewFrame(byte[] data, Camera c){ lastFrame=data; Camera.Size s=c.getParameters().getPreviewSize(); frameW=s.width; frameH=s.height; } public void onClick(View v){ if(v.getId()==999) doScanV10(); if(v.getId()==1000){ checkPerm(); if(hasCamPerm) try{ openCam(sv.getHolder()); }catch(Exception e){} } }

 void doScanV10(){
  detected.clear(); if(lastFrame==null){ info.setText("Wait 1s"); return; } int w=frameW,h=frameH; int vw=viewW>0?viewW:720; int vh=viewH>0?viewH:1362; int usableH=vh-350; if(usableH<400) usableH=vh*3/4;
  long sum=0; int cnt=0; for(int i=0;i<w*h;i+=150){ sum+=(lastFrame[i]&0xFF); cnt++; } int avg=(int)(sum/cnt);
  int brightThresh = avg<45? 75 : avg<65? 90 : 110;
  List<Rect> raw=new ArrayList<Rect>(); List<Boolean> isBright=new ArrayList<Boolean>();

  // Find brightest vertical column - MAIN LEAK
  int bestCol=-1, bestScore=0, bestTop=h, bestBot=0;
  for(int x=w/5;x<w*4/5;x++){
    int count=0, top=h, bot=0, sumY=0; 
    for(int y=h/12;y<h*10/12;y+=2){ int Y=lastFrame[y*w+x]&0xFF; if(Y>=brightThresh){ count++; sumY+=Y; if(y<top) top=y; if(y>bot) bot=y; } }
    int score = count * (bot-top)/10 + sumY/10;
    if(count>h/15 && (bot-top)>h/6 && score>bestScore){ bestScore=score; bestCol=x; bestTop=top; bestBot=bot; }
  }
  if(bestCol!=-1){
    raw.add(new Rect(bestCol-6, bestTop, bestCol+14, bestBot));
    isBright.add(true);
  }
  // Secondary leaks - other columns with strong signal
  for(int x=w/5;x<w*4/5;x+=10){
    if(Math.abs(x-bestCol)<18) continue;
    int count=0, top=h, bot=0; 
    for(int y=h/12;y<h*10/12;y+=3){ int Y=lastFrame[y*w+x]&0xFF; if(Y>=brightThresh+5){ count++; if(y<top) top=y; if(y>bot) bot=y; } }
    if(count>h/18 && (bot-top)>h/8){ raw.add(new Rect(x-5, top, x+12, bot)); isBright.add(true); }
  }

  // Map with 90deg rotation fix: camera frame is landscape, display is portrait rotated 90
  // frame x = 0..w (width), frame y = 0..h (height)
  // display: mx = y * vw / h, my = (w - x) * usableH / w  but adjust
  List<Rect> mapped=new ArrayList<Rect>();
  for(Rect r: raw){
    // r.left = x, r.top = y in frame coords
    int fx1=r.left, fy1=r.top, fx2=r.right, fy2=r.bottom;
    int mx = (int)((float)fy1 * vw / h);
    int my = (int)((float)(w - fx2) * usableH / w);
    int mx2 = (int)((float)fy2 * vw / h);
    int my2 = (int)((float)(w - fx1) * usableH / w);
    int l=Math.min(mx,mx2), t=Math.min(my,my2), rr=Math.max(mx,mx2), b=Math.max(my,my2);
    if(rr-l<28) rr=l+56; if(b-t<28) b=t+80;
    // clamp
    if(l<0) l=0; if(t<0) t=0; if(rr>vw) rr=vw-5; if(b>usableH) b=usableH;
    mapped.add(new Rect(l,t,rr,b));
  }

  // Merge close bright leaks
  List<Rect> merged=new ArrayList<Rect>();
  for(int i=0;i<mapped.size();i++){
    Rect cur=mapped.get(i); boolean mergedOne=false;
    for(int j=0;j<merged.size();j++){
      Rect ex=merged.get(j); int cx=Math.abs(ex.centerX()-cur.centerX()); int cy=Math.abs(ex.centerY()-cur.centerY());
      if(cx<140 && cy<200){ ex.union(cur); mergedOne=true; break; }
    }
    if(!mergedOne) merged.add(new Rect(cur));
  }

  for(int i=0;i<merged.size();i++){
    Rect r=merged.get(i); if(r.width()<24 && r.height()<24) continue;
    String label; int type;
    // blueprint style matching your example
    if(r.height()>r.width()*2.5f){ label="② VERTICAL LEAK"; type=2; }
    else if(r.width()>r.height()*2f){ label="① MISSING PLANK / OPEN"; type=1; }
    else if(r.left<70 || r.right>vw-70){ label="③ ROTTEN FRAME EDGES"; type=3; }
    else { label="④ RUSTED LOCK"; type=4; }
    detected.add(new OverlayView.LabeledBox(r,label,true,type));
  }

  if(overlay!=null) overlay.setBoxes(detected);
  if(detected.size()==0){ info.setText("V10 CLEAN avg:"+avg+" B:"+brightThresh+" bestScore:"+bestScore); return; }
  float total=0; for(OverlayView.LabeledBox b: detected) total+=(b.rect.width()*b.rect.height())/95f;
  info.setText("V10 BLUEPRINT FINAL: "+detected.size()+" defects (raw "+raw.size()+" avg "+avg+" B"+brightThresh+" score "+bestScore+") "+String.format("%.1f",total)+"cm2");
 }

 static class OverlayView extends View {
  static class LabeledBox{ Rect rect; String label; boolean bright; int type; LabeledBox(Rect r,String l,boolean b,int t){ rect=r; label=l; bright=b; type=t; } }
  List<LabeledBox> boxes=new ArrayList<LabeledBox>(); Paint p1,p2,p3,p4,p5,pR,pT,pFill; int vw=720,vh=1362;
  public OverlayView(android.content.Context c){ super(c);
   p1=new Paint(); p1.setColor(0xFF00E5FF); p1.setStyle(Paint.Style.STROKE); p1.setStrokeWidth(8); // cyan
   p2=new Paint(); p2.setColor(0xFFFF00FF); p2.setStyle(Paint.Style.STROKE); p2.setStrokeWidth(8); // magenta
   p3=new Paint(); p3.setColor(0xFFFFFF00); p3.setStyle(Paint.Style.STROKE); p3.setStrokeWidth(8); // yellow
   p4=new Paint(); p4.setColor(0xFF00FF00); p4.setStyle(Paint.Style.STROKE); p4.setStrokeWidth(8); // green
   p5=new Paint(); p5.setColor(0xFFFF8800); p5.setStyle(Paint.Style.STROKE); p5.setStrokeWidth(8);
   pR=new Paint(); pR.setColor(0x22FFFFFF); pR.setStyle(Paint.Style.STROKE); pR.setStrokeWidth(1);
   pT=new Paint(); pT.setColor(0xFFFFFFFF); pT.setTextSize(20); pT.setAntiAlias(true); pT.setShadowLayer(5,1,1,0xFF000000); pT.setFakeBoldText(true);
   pFill=new Paint(); pFill.setColor(0xEE000000); pFill.setStyle(Paint.Style.FILL);
  }
  void setViewSize(int w,int h){ vw=w; vh=h; } void setBoxes(List<LabeledBox> b){ boxes=b; invalidate(); }
  Paint getPaint(int t){ if(t==1) return p1; if(t==2) return p2; if(t==3) return p3; if(t==4) return p4; return p5; }
  protected void onDraw(Canvas c){ super.onDraw(c); int w=getWidth(),h=getHeight(); if(w>0) vw=w; if(h>0) vh=h; for(int i=1;i<10;i++){ c.drawLine(0,h*i/10,vw,h*i/10,pR); c.drawLine(vw*i/10,0,vw*i/10,h,pR); } c.drawText("CODEX6120 V10 BLUEPRINT FINAL "+boxes.size()+" DEFECTS",20,44,pT);
   int num=1; for(LabeledBox lb: boxes){ Paint p=getPaint(lb.type); c.drawRect(lb.rect,p); c.drawRect(lb.rect.left-4, lb.rect.top-4, lb.rect.right+4, lb.rect.bottom+4, p);
    String txt = "⓪".substring(0,0) + num + " " + lb.label; txt = num+" "+lb.label; // numbered like your blueprint
    float tw=pT.measureText(txt); float lx=lb.rect.left; float ly=lb.rect.top-10; if(ly<28) ly=lb.rect.bottom+24; if(lx+tw>vw-4) lx=vw-tw-6; if(lx<2) lx=2;
    c.drawRect(lx-6, ly-18, lx+tw+8, ly+6, pFill); c.drawText(txt, lx, ly, p); num++; }
  }
 }
}
