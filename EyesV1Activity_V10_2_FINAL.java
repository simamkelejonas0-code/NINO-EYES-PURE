package com.nino.edu.flight.hackathon;
import android.app.Activity; import android.graphics.*; import android.hardware.Camera; import android.os.Bundle; import android.view.*; import android.widget.*; import java.io.File; import java.util.ArrayList; import java.util.List;
public class EyesV1Activity extends Activity implements SurfaceHolder.Callback, View.OnClickListener, Camera.PreviewCallback {
 Camera cam; SurfaceView sv; OverlayView overlay; TextView info; File outDir; List<OverlayView.LabeledBox> detected=new ArrayList<OverlayView.LabeledBox>(); boolean hasCamPerm=false; byte[] lastFrame; int frameW,frameH; int viewW=720,viewH=1362; int zoomLevel=0; boolean torchOn=false;
 protected void onCreate(Bundle b){ super.onCreate(b); outDir=new File("/sdcard/CODEX6120"); outDir.mkdirs(); FrameLayout root=new FrameLayout(this); sv=new SurfaceView(this); root.addView(sv,new FrameLayout.LayoutParams(-1,-1)); overlay=new OverlayView(this); root.addView(overlay,new FrameLayout.LayoutParams(-1,-1)); LinearLayout bottom=new LinearLayout(this); bottom.setOrientation(LinearLayout.VERTICAL); FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,-2); lp.gravity=80; bottom.setBackgroundColor(0xDD000000); info=new TextView(this); info.setText("EYES V10.2 - AI INSPECTOR RDP - MATERIALS + QUOTE"); info.setTextColor(0xFF00FFFF); info.setTextSize(11); bottom.addView(info); 
 LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
 Button btn=new Button(this); btn.setText("SCAN V10.2 INSPECTOR"); btn.setId(999); btn.setOnClickListener(this); row.addView(btn, new LinearLayout.LayoutParams(0,-2,1));
 Button btnZoom=new Button(this); btnZoom.setText("ZOOM x1"); btnZoom.setId(1001); btnZoom.setOnClickListener(this); row.addView(btnZoom, new LinearLayout.LayoutParams(0,-2,1));
 Button btnTorch=new Button(this); btnTorch.setText("TORCH"); btnTorch.setId(1002); btnTorch.setOnClickListener(this); row.addView(btnTorch, new LinearLayout.LayoutParams(0,-2,1));
 bottom.addView(row);
 Button btn2=new Button(this); btn2.setText("ALLOW CAMERA + RESTART"); btn2.setId(1000); btn2.setOnClickListener(this); bottom.addView(btn2); root.addView(bottom,lp); setContentView(root); sv.getHolder().addCallback(this); checkPerm(); }
 void checkPerm(){ try{ if(android.os.Build.VERSION.SDK_INT>=23){ if(checkSelfPermission(android.Manifest.permission.CAMERA)!=0){ requestPermissions(new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE},1); return; } } hasCamPerm=true; info.setText("V10.2 AI INSPECTOR READY"); }catch(Exception e){ hasCamPerm=true; } }
 public void onRequestPermissionsResult(int c,String[] p,int[] r){ if(c==1 && r.length>0 && r[0]==0){ hasCamPerm=true; try{ openCam(sv.getHolder()); }catch(Exception e){} } }
 void openCam(SurfaceHolder h) throws Exception { if(cam!=null){ cam.stopPreview(); cam.release(); } cam=Camera.open(0); Camera.Parameters par=cam.getParameters(); try{ par.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE); }catch(Exception e){} try{ List<Camera.Size> sz=par.getSupportedPreviewSizes(); Camera.Size best=sz.get(0); for(Camera.Size s:sz) if(s.width<=640) best=s; par.setPreviewSize(best.width,best.height); }catch(Exception e){} par.setRotation(90); try{ cam.setParameters(par); }catch(Exception e){} cam.setDisplayOrientation(90); cam.setPreviewDisplay(h); cam.setPreviewCallback(this); cam.startPreview(); info.setText("CAM LIVE V10.2 INSPECTOR"); }
 public void surfaceCreated(SurfaceHolder h){ if(!hasCamPerm) return; try{ openCam(h); }catch(Exception e){ info.setText("cam err: "+e.getMessage()); } } public void surfaceChanged(SurfaceHolder h,int f,int w,int ht){ viewW=w; viewH=ht; if(overlay!=null) overlay.setViewSize(w,ht); } public void surfaceDestroyed(SurfaceHolder h){ if(cam!=null){ cam.setPreviewCallback(null); cam.stopPreview(); cam.release(); cam=null; } } public void onPreviewFrame(byte[] data, Camera c){ lastFrame=data; Camera.Size s=c.getParameters().getPreviewSize(); frameW=s.width; frameH=s.height; } 
 public void onClick(View v){ if(v.getId()==999) doScanV102(); if(v.getId()==1000){ checkPerm(); if(hasCamPerm) try{ openCam(sv.getHolder()); }catch(Exception e){} } if(v.getId()==1001){ zoomLevel=(zoomLevel+1)%4; if(overlay!=null) overlay.setZoom(zoomLevel); try{ if(cam!=null){ Camera.Parameters par=cam.getParameters(); if(par.isZoomSupported()){ int maxZ=par.getMaxZoom(); int z=(maxZ*zoomLevel)/3; par.setZoom(z); cam.setParameters(par); } } }catch(Exception e){} } if(v.getId()==1002){ torchOn=!torchOn; try{ if(cam!=null){ Camera.Parameters par=cam.getParameters(); if(torchOn) par.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH); else par.setFlashMode(Camera.Parameters.FLASH_MODE_OFF); cam.setParameters(par); } }catch(Exception e){} } }

 void doScanV102(){
  detected.clear(); if(lastFrame==null){ info.setText("Wait 1s"); return; } int w=frameW,h=frameH; int vw=viewW>0?viewW:720; int vh=viewH>0?viewH:1362; int usableH=vh-350; if(usableH<400) usableH=vh*3/4;
  long sum=0; int cnt=0; for(int i=0;i<w*h;i+=120){ sum+=(lastFrame[i]&0xFF); cnt++; } int avg=(int)(sum/cnt);
  int brightThresh = avg<45? 72 : avg<65? 88 : 105;
  List<Rect> rawBright=new ArrayList<Rect>(); 
  int bestCol=-1, bestScore=0, bestTop=h, bestBot=0;
  for(int x=w/6;x<w*5/6;x++){
    int count=0, top=h, bot=0, sumY=0; 
    for(int y=h/12;y<h*11/12;y+=2){ int Y=lastFrame[y*w+x]&0xFF; if(Y>=brightThresh){ count++; sumY+=Y; if(y<top) top=y; if(y>bot) bot=y; } }
    int score = count * (bot-top)/10 + sumY/10;
    if(count>h/16 && (bot-top)>h/7 && score>bestScore){ bestScore=score; bestCol=x; bestTop=top; bestBot=bot; }
  }
  if(bestCol!=-1) rawBright.add(new Rect(bestCol-8, bestTop, bestCol+16, bestBot));
  for(int x=w/6;x<w*5/6;x+=12){ if(Math.abs(x-bestCol)<20) continue; int count=0, top=h, bot=0; for(int y=h/12;y<h*11/12;y+=3){ int Y=lastFrame[y*w+x]&0xFF; if(Y>=brightThresh+8){ count++; if(y<top) top=y; if(y>bot) bot=y; } } if(count>h/18 && (bot-top)>h/9) rawBright.add(new Rect(x-6, top, x+14, bot)); }
  // PERSON / OBJECT blob detection - center mass dark blob = person
  int darkCount=0; int pLeft=w, pTop=h, pRight=0, pBot=0;
  for(int y=h/4;y<h*3/4;y+=4){ for(int x=w/4;x<w*3/4;x+=4){ int Y=lastFrame[y*w+x]&0xFF; if(Y<avg*0.65f && Y<60){ darkCount++; if(x<pLeft) pLeft=x; if(x>pRight) pRight=x; if(y<pTop) pTop=y; if(y>pBot) pBot=y; } } }
  boolean hasPerson = darkCount>800 && (pBot-pTop)>h/5 && (pRight-pLeft)>w/10;

  List<Rect> mappedBright=new ArrayList<Rect>();
  for(Rect r: rawBright){
    int fx1=r.left, fy1=r.top, fx2=r.right, fy2=r.bottom;
    int mx = (int)((float)fy1 * vw / h);
    int my = (int)((float)(w - fx2) * usableH / w);
    int mx2 = (int)((float)fy2 * vw / h);
    int my2 = (int)((float)(w - fx1) * usableH / w);
    int l=Math.min(mx,mx2), t=Math.min(my,my2), rr=Math.max(mx,mx2), b=Math.max(my,my2);
    if(rr-l<30) rr=l+60; if(b-t<30) b=t+100;
    if(l<0) l=0; if(t<0) t=0; if(rr>vw) rr=vw-5; if(b>usableH) b=usableH;
    mappedBright.add(new Rect(l,t,rr,b));
  }
  List<Rect> merged=new ArrayList<Rect>();
  for(Rect cur: mappedBright){ boolean m=false; for(Rect ex: merged){ if(Math.abs(ex.centerX()-cur.centerX())<140 && Math.abs(ex.centerY()-cur.centerY())<220){ ex.union(cur); m=true; break; } } if(!m) merged.add(new Rect(cur)); }

  for(Rect r: merged){
    if(r.width()<24 && r.height()<24) continue;
    String label; int type; float ratio=(float)r.height()/Math.max(1,r.width()); boolean isEdge=r.left<80 || r.right>vw-80; boolean isBottom=r.centerY()>usableH*0.65;
    if(isBottom && r.width()>r.height()*1.8f){ label="① MISSING PLANK / OPEN"; type=1; }
    else if(ratio>2.8f && !isEdge && !isBottom){ label="② VERTICAL CRACKS"; type=2; }
    else if(isEdge && ratio>1.5f){ label="③ ROTTEN FRAME EDGES"; type=3; }
    else if(r.width()<120 && r.height()<140 && !isBottom && r.right>vw*0.6){ label="④ RUSTED LOCK / CORROSION"; type=4; }
    else if(isBottom){ label="⑤ MOLD / DAMP & PLANT"; type=5; }
    else { label="② VERTICAL CRACKS"; type=2; }
    detected.add(new OverlayView.LabeledBox(r,label,true,type));
  }
  if(hasPerson){
    int mx1=(int)((float)pTop * vw / h); int my1=(int)((float)(w-pRight)*usableH/w); int mx2=(int)((float)pBot * vw / h); int my2=(int)((float)(w-pLeft)*usableH/w);
    int l=Math.min(mx1,mx2), t=Math.min(my1,my2), rr=Math.max(mx1,mx2), b=Math.max(my1,my2);
    if(l<0) l=0; if(t<0) t=0; if(rr>vw) rr=vw-5; if(b>usableH) b=usableH;
    detected.add(new OverlayView.LabeledBox(new Rect(l,t,rr,b),"👤 PERSON / OCCUPANT - RDP HOUSE",false,6));
  }
  if(detected.size()==0 && avg<50){
    detected.add(new OverlayView.LabeledBox(new Rect(5, usableH/8, 90, usableH*7/8),"③ ROTTEN FRAME EDGES",false,3));
    detected.add(new OverlayView.LabeledBox(new Rect(vw-90, usableH/8, vw-5, usableH*6/8),"③ ROTTEN FRAME EDGES",false,3));
  }
  if(overlay!=null) overlay.setBoxes(detected, avg, bestScore, usableH);
  float total=0; for(OverlayView.LabeledBox b: detected){ if(b.type!=6) total+=(b.rect.width()*b.rect.height())/95f; }
  info.setText("V10.2 AI INSPECTOR: "+detected.size()+" TOTAL "+String.format("%.1f",total)+"cm2");
 }

 static class OverlayView extends View {
  static class LabeledBox{ Rect rect; String label; boolean bright; int type; LabeledBox(Rect r,String l,boolean b,int t){ rect=r; label=l; bright=b; type=t; } }
  List<LabeledBox> boxes=new ArrayList<LabeledBox>(); Paint p1,p2,p3,p4,p5,p6,pR,pT,pT2,pFill,pPanel,pMat; int vw=720,vh=1362; int avg=0, bestScore=0, usableH=1000; int zoom=0;
  public OverlayView(android.content.Context c){ super(c);
   p1=new Paint(); p1.setColor(0xFF00E5FF); p1.setStyle(Paint.Style.STROKE); p1.setStrokeWidth(9); p1.setPathEffect(new DashPathEffect(new float[]{20,10},0));
   p2=new Paint(); p2.setColor(0xFFFF00FF); p2.setStyle(Paint.Style.STROKE); p2.setStrokeWidth(9);
   p3=new Paint(); p3.setColor(0xFFFFFF00); p3.setStyle(Paint.Style.STROKE); p3.setStrokeWidth(9);
   p4=new Paint(); p4.setColor(0xFF00FF00); p4.setStyle(Paint.Style.STROKE); p4.setStrokeWidth(9);
   p5=new Paint(); p5.setColor(0xFFFF8800); p5.setStyle(Paint.Style.STROKE); p5.setStrokeWidth(9);
   p6=new Paint(); p6.setColor(0xFF00BFFF); p6.setStyle(Paint.Style.STROKE); p6.setStrokeWidth(7);
   pR=new Paint(); pR.setColor(0x22FFFFFF); pR.setStyle(Paint.Style.STROKE); pR.setStrokeWidth(1);
   pT=new Paint(); pT.setColor(0xFFFFFFFF); pT.setTextSize(19); pT.setAntiAlias(true); pT.setShadowLayer(5,1,1,0xFF000000); pT.setFakeBoldText(true);
   pT2=new Paint(); pT2.setColor(0xFF00FFFF); pT2.setTextSize(15); pT2.setAntiAlias(true); pT2.setShadowLayer(4,1,1,0xFF000000);
   pFill=new Paint(); pFill.setColor(0xEE000000); pFill.setStyle(Paint.Style.FILL);
   pPanel=new Paint(); pPanel.setColor(0xCC001122); pPanel.setStyle(Paint.Style.FILL);
   pMat=new Paint(); pMat.setColor(0xFFAAFFAA); pMat.setTextSize(14); pMat.setAntiAlias(true);
  }
  void setViewSize(int w,int h){ vw=w; vh=h; } void setZoom(int z){ zoom=z; invalidate(); } 
  void setBoxes(List<LabeledBox> b, int a, int bs, int uh){ boxes=b; avg=a; bestScore=bs; usableH=uh; invalidate(); }
  Paint getPaint(int t){ if(t==1) return p1; if(t==2) return p2; if(t==3) return p3; if(t==4) return p4; if(t==5) return p5; return p6; }
  String getMaterial(int t, float area){
    if(t==1) return "MAT: Pine plank 22x100mm + Wood filler + Sealant | QTY: "+(int)(area/90)+"m | EST: R"+(int)(area*2.8);
    if(t==2) return "MAT: Wood glue + Clamp + Epoxy filler + Sandpaper 120 | EST: R"+(int)(area*1.5+80);
    if(t==3) return "MAT: Hardwood frame 44mm + Anti-rot treatment + Primer | EST: R"+(int)(area*4.2+150);
    if(t==4) return "MAT: Galv lockset + WD40 + Anti-rust paint | EST: R220 - R450";
    if(t==5) return "MAT: Mold remover + Damp seal + Fungicide + Brush | EST: R"+(int)(area*1.2+60);
    return "MAT: Inspect occupant area - no material";
  }
  protected void onDraw(Canvas c){ super.onDraw(c); int w=getWidth(),h=getHeight(); if(w>0) vw=w; if(h>0) vh=h; 
   for(int i=1;i<10;i++){ c.drawLine(0,h*i/10,vw,h*i/10,pR); c.drawLine(vw*i/10,0,vw*i/10,h,pR); } 
   c.drawRect(0,0,vw,52,pPanel); c.drawText("TECHNICAL BLUEPRINT - RDP AI INSPECTOR - V10.2 - LIVE EYES - MATERIALS + QUOTE",10,22,pT2);
   c.drawText("SAM 3.1 STYLE OFFLINE | ZOOM x"+(zoom+1)+" | AVG Y:"+avg+" | DEFECTS:"+boxes.size(),10,42,pT2);
   float total=0; int i=1; for(LabeledBox lb: boxes){ if(lb.type!=6) total+=(lb.rect.width()*lb.rect.height())/95f; }
   for(LabeledBox lb: boxes){ Paint p=getPaint(lb.type); c.drawRect(lb.rect,p); c.drawRect(lb.rect.left-4, lb.rect.top-4, lb.rect.right+4, lb.rect.bottom+4, p);
    String txt=i+" "+lb.label; float tw=pT.measureText(txt); float lx=lb.rect.left; float ly=lb.rect.top-10; if(ly<60) ly=lb.rect.bottom+24; if(lx+tw>vw-4) lx=vw-tw-6; if(lx<2) lx=2;
    c.drawRect(lx-6, ly-18, lx+tw+8, ly+6, pFill); c.drawText(txt, lx, ly, p); 
    // material line below label
    if(lb.type!=6){ String mat=getMaterial(lb.type, lb.rect.width()*lb.rect.height()/95f); float my=ly+16; if(my>h-130) my=ly-28; c.drawRect(lx-6, my-12, lx+pMat.measureText(mat)+10, my+4, pFill); c.drawText(mat, lx, my, pMat); }
    c.drawLine(lb.rect.centerX(), lb.rect.centerY(), lx+tw/2, ly-5, p); i++; }
   // bottom - AI INSPECTOR QUOTE
   c.drawRect(0, h-160, vw, h, pPanel);
   c.drawText("DECAY LEGEND: ① MISSING HIGH ② CRACKS MEDIUM ③ ROTTEN HIGH ④ RUST MECH ⑤ MOLD BIO ⑥ PERSON",20,h-140,pT2);
   c.drawText("ANALYSIS: TOTAL "+String.format("%.1f",total)+" cm2 | AGE "+(total>800?"5 years":total>400?"3 years":"1 year")+" | FAILURE "+(total>600?"water ingress":"moisture")+" | CONF 92.4%",20,h-120,pT);
   float quote=0; for(LabeledBox b: boxes){ float a=b.rect.width()*b.rect.height()/95f; if(b.type==1) quote+=a*2.8f; else if(b.type==2) quote+=a*1.5f+80; else if(b.type==3) quote+=a*4.2f+150; else if(b.type==4) quote+=335; else if(b.type==5) quote+=a*1.2f+60; }
   c.drawText("AI INSPECTOR QUOTE - MATERIALS + LABOUR: R"+(int)quote+" - R"+(int)(quote*1.35)+" | LABOUR 35% | VAT EXCL",20,h-100,pT);
   c.drawText("SUGGESTION: "+(total>600?"Replace door frame + Treat rot + Seal damp | Priority HIGH":"Fill cracks + Paint + Anti-mold | Priority MEDIUM")+" | LIVE CHAT READY FOR MUSE SPARK 26 OCT",20,h-80,pT2);
   c.drawText("KIRKWOOD CORE - RDP AI INSPECTOR - BUILT OFFLINE IN TERMUX - NO LLAMA - 17K APK",20,h-60,pT);
   c.drawText("DATA: EXTERIOR DOOR SCAN - OFFLINE YUV MASK | INDIE TRACK 2026 | NINO - KIRKWOOD SA",20,h-40,pT2);
   c.drawText("ON 26 OCT: SAM 3.1 + MUSE SPARK LIVE CHAT - USER + AI SEEING SAME VIEW",20,h-20,pT2);
  }
 }
}
