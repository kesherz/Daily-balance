package com.example.dailycandle;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private final ArrayList<Double> values = new ArrayList<>();
    private final ArrayList<String> dates = new ArrayList<>();
    private SharedPreferences prefs;
    private EditText input;
    private CandleChartView chart;
    private TextView info;
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy/MM/dd", Locale.US);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(9,11,16));
        getWindow().setNavigationBarColor(Color.rgb(9,11,16));
        prefs = getSharedPreferences("data", MODE_PRIVATE);
        load();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(9,11,16));
        int p = dp(10);
        root.setPadding(p, dp(5), p, dp(8));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("Daily Candle"); title.setTextSize(20); title.setTypeface(Typeface.DEFAULT, Typeface.BOLD); title.setTextColor(Color.WHITE);
        title.setPadding(dp(6),dp(3),0,dp(5));
        top.addView(title, new LinearLayout.LayoutParams(0,-2,1f));
        Button reset = new Button(this); reset.setText("↺"); reset.setTextSize(20); reset.setTextColor(Color.WHITE); reset.setMinWidth(0); reset.setPadding(0,0,0,0);
        reset.setOnClickListener(v -> { chart.resetView(); });
        top.addView(reset, new LinearLayout.LayoutParams(dp(52),dp(48)));
        root.addView(top);

        chart = new CandleChartView(this, values, (index, open, close, date) -> {
            String dir = close > open ? "صعودی" : close < open ? "نزولی" : "بدون تغییر";
            info.setText("روز " + (index + 1) + "  •  " + date + "    O " + fmtNum(open) + "   C " + fmtNum(close) + "   " + dir);
        });
        root.addView(chart, new LinearLayout.LayoutParams(-1,0,1f));

        info = new TextView(this);
        info.setText("برای زوم: دو انگشت • برای جابه‌جایی: یک انگشت • دو ضربه: بازنشانی");
        info.setTextSize(11); info.setTextColor(Color.LTGRAY); info.setPadding(dp(6),dp(5),dp(6),dp(5));
        root.addView(info, new LinearLayout.LayoutParams(-1,-2));

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        input = new EditText(this);
        input.setHint("عدد امروز"); input.setTextSize(17); input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        input.setTextColor(Color.WHITE); input.setHintTextColor(Color.GRAY); input.setBackgroundColor(Color.rgb(24,27,34)); input.setPadding(dp(18),0,dp(12),0);
        row.addView(input,new LinearLayout.LayoutParams(0,dp(56),1f));
        Button btn = new Button(this); btn.setText("ثبت"); btn.setTextColor(Color.WHITE); btn.setBackgroundColor(Color.rgb(40,140,70)); btn.setOnClickListener(v -> addValue());
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(88),dp(56)); bp.setMarginStart(dp(8)); row.addView(btn,bp);
        root.addView(row);

        TextView hint = new TextView(this);
        hint.setText("Open = عدد روز قبل   •   Close = عدد امروز   •   بدون Shadow"); hint.setTextSize(11); hint.setTextColor(Color.GRAY); hint.setPadding(dp(6),dp(5),dp(6),0);
        root.addView(hint);
        setContentView(root);
    }

    private void addValue(){
        String s=input.getText().toString().replace(',','.');
        double v;
        try{v=Double.parseDouble(s);}catch(Exception e){input.setError("یک عدد معتبر وارد کن");return;}
        if(!values.isEmpty() && v==values.get(values.size()-1)) Toast.makeText(this,"کندل بدون تغییر ثبت شد",Toast.LENGTH_SHORT).show();
        values.add(v); dates.add(fmt.format(new Date())); save(); chart.keepLatestVisible(); chart.invalidate(); input.setText("");
        InputMethodManager imm=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE); if(imm!=null)imm.hideSoftInputFromWindow(input.getWindowToken(),0);
    }
    private String fmtNum(double n){ return String.format(Locale.US,"%.4f",n).replaceAll("0+$","").replaceAll("\\.$",""); }
    private void save(){StringBuilder v=new StringBuilder(),d=new StringBuilder();for(int i=0;i<values.size();i++){if(i>0){v.append(',');d.append(',');}v.append(values.get(i));d.append(dates.get(i));}prefs.edit().putString("values",v.toString()).putString("dates",d.toString()).apply();}
    private void load(){String v=prefs.getString("values","");if(!v.isEmpty())for(String s:v.split(","))try{values.add(Double.parseDouble(s));}catch(Exception ignored){} String d=prefs.getString("dates","");if(!d.isEmpty())for(String s:d.split(","))dates.add(s);}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}

    interface Listener { void onSelected(int index,double open,double close,String date); }

    static class CandleChartView extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private final Paint text=new Paint(Paint.ANTI_ALIAS_FLAG); private final List<Double> values; private final Listener listener;
        private float zoom=1f, offset=0f, lastX,lastY,downX,downY; private boolean drag=false,pinch=false; private float pinchStartDist,pinchStartZoom; private long downTime; private int selected=-1;
        private final float minZoom=0.45f,maxZoom=4.5f;
        CandleChartView(Context c,List<Double> v,Listener l){super(c);values=v;listener=l;setBackgroundColor(Color.rgb(13,15,20));text.setTypeface(Typeface.DEFAULT);setFocusable(true);}
        private float dp(float x){return x*getResources().getDisplayMetrics().density;}
        private float clamp(float x,float a,float b){return Math.max(a,Math.min(b,x));}
        private float distance(MotionEvent e){if(e.getPointerCount()<2)return 0;float dx=e.getX(0)-e.getX(1),dy=e.getY(0)-e.getY(1);return (float)Math.sqrt(dx*dx+dy*dy);}
        private double[] visibleRange(float gap,float left){
            int n=values.size(); if(n<=1)return new double[]{0,1};
            int first=Math.max(0,(int)Math.floor((-left-gap)/gap)-1); int last=Math.min(n-1,(int)Math.ceil((getWidth()-left+gap)/gap)+1);
            double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
            for(int i=Math.max(0,first);i<=last;i++){double v=values.get(i);min=Math.min(min,v);max=Math.max(max,v);}
            if(!Double.isFinite(min)){min=values.get(0);max=min;} if(max-min<1e-12){double d=Math.max(1.0,Math.abs(min)*0.01);min-=d;max+=d;}
            return new double[]{min,max};
        }
        @Override protected void onDraw(Canvas c){
            super.onDraw(c); int w=getWidth(),h=getHeight(); if(values.isEmpty()){text.setColor(Color.GRAY);text.setTextSize(dp(16));c.drawText("اولین عدد را وارد کن",w/2f-dp(65),h/2f,text);return;}
            float top=dp(28),bottom=h-dp(32),chartH=Math.max(dp(40),bottom-top); float gap=dp(62)*zoom; float left=dp(34)+offset; float bodyW=clamp(dp(32)*zoom,dp(8),dp(58));
            double[] vr=visibleRange(gap,left); double min=vr[0],max=vr[1],range=max-min; double pad=range*0.10; double lo=min-pad,hi=max+pad;
            // grid + price scale
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));p.setColor(Color.rgb(38,41,49));
            text.setColor(Color.rgb(145,148,157));text.setTextSize(dp(10));
            for(int i=0;i<=6;i++){float y=top+chartH*i/6f;c.drawLine(0,y,w,y,p);double pv=hi-(hi-lo)*i/6.0;c.drawText(formatPrice(pv),dp(4),y-dp(3),text);}
            // vertical grid
            for(int i=0;i<values.size();i++){float x=left+i*gap;if(x<0||x>w)continue;p.setColor(Color.rgb(30,33,40));c.drawLine(x,top,x,bottom,p);}
            // candles
            for(int i=1;i<values.size();i++){
                float x=left+i*gap;if(x<-bodyW||x>w+bodyW)continue;double open=values.get(i-1),close=values.get(i);float yo=(float)(top+(hi-open)/(hi-lo)*chartH),yc=(float)(top+(hi-close)/(hi-lo)*chartH);float y1=Math.min(yo,yc),y2=Math.max(yo,yc);if(y2-y1<dp(2))y2=y1+dp(2);
                p.setStyle(Paint.Style.FILL);p.setColor(close>=open?Color.rgb(38,190,90):Color.rgb(230,70,70));c.drawRect(x-bodyW/2,y1,x+bodyW/2,y2,p);
                if(i==selected){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));p.setColor(Color.WHITE);c.drawRect(x-bodyW/2-dp(2),y1-dp(2),x+bodyW/2+dp(2),y2+dp(2),p);}
            }
            // selected crosshair
            if(selected>0 && selected<values.size()){
                float x=left+selected*gap; double cv=values.get(selected);float y=(float)(top+(hi-cv)/(hi-lo)*chartH);
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));p.setColor(Color.rgb(190,190,195));c.drawLine(x,top,x,bottom,p);c.drawLine(0,y,w,y,p);
                p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(35,38,46));c.drawRect(0,y-dp(11),dp(72),y+dp(5),p);text.setColor(Color.WHITE);text.setTextSize(dp(10));c.drawText(formatPrice(cv),dp(4),y+dp(1),text);
            }
            // x labels, last visible few
            text.setColor(Color.rgb(145,148,157));text.setTextSize(dp(10));for(int i=1;i<values.size();i++){float x=left+i*gap;if(x>=0&&x<=w) c.drawText(String.valueOf(i+1),x-dp(5),h-dp(10),text);}
        }
        private String formatPrice(double v){if(Math.abs(v)>=100)return String.format(Locale.US,"%.1f",v);if(Math.abs(v)>=1)return String.format(Locale.US,"%.2f",v);return String.format(Locale.US,"%.4f",v);}
        void resetView(){zoom=1f;offset=0f;selected=-1;invalidate();}
        void keepLatestVisible(){ if(values.size()>1){float gap=dp(62)*zoom;float target=getWidth()-dp(90);float x=dp(34)+(values.size()-1)*gap;offset+=target-x;invalidate();}}
        private int nearestIndex(float x){float gap=dp(62)*zoom,left=dp(34)+offset;int i=Math.round((x-left)/gap);return i;}
        @Override public boolean onTouchEvent(MotionEvent e){
            switch(e.getActionMasked()){
                case MotionEvent.ACTION_DOWN: downX=lastX=e.getX();downY=lastY=e.getY();downTime=System.currentTimeMillis();drag=true;pinch=false;return true;
                case MotionEvent.ACTION_POINTER_DOWN: if(e.getPointerCount()>=2){pinch=true;drag=false;pinchStartDist=distance(e);pinchStartZoom=zoom;}return true;
                case MotionEvent.ACTION_MOVE:
                    if(pinch && e.getPointerCount()>=2){float d=distance(e);if(pinchStartDist>0){float nz=clamp(pinchStartZoom*(d/pinchStartDist),minZoom,maxZoom);float oldGap=dp(62)*zoom,newGap=dp(62)*nz;float cx=(e.getX(0)+e.getX(1))/2f;float index=(cx-(dp(34)+offset))/oldGap;offset=cx-dp(34)-index*newGap;zoom=nz;invalidate();}return true;}
                    if(drag){float dx=e.getX()-lastX;offset+=dx;lastX=e.getX();invalidate();}return true;
                case MotionEvent.ACTION_POINTER_UP: pinch=false;drag=false;return true;
                case MotionEvent.ACTION_UP:
                    long dt=System.currentTimeMillis()-downTime;float moved=Math.abs(e.getX()-downX)+Math.abs(e.getY()-downY);
                    if(!pinch && moved<dp(12) && dt<250){int i=nearestIndex(e.getX());if(i>=1&&i<values.size()){selected=i;listener.onSelected(i,values.get(i-1),values.get(i),"روز "+(i+1));invalidate();}}
                    else if(!pinch && dt<300 && moved<dp(12)){resetView();}
                    drag=false;return true;
            }return true;
        }
    }
}
