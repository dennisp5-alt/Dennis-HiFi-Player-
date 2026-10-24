package com.dennis.hifiplayer;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.Holder> {
    public interface OnSongClick { void open(Song song); }
    private final List<Song> songs=new ArrayList<>();
    private final OnSongClick click;
    private String currentId="";
    private final int light=Color.rgb(237,243,248), dim=Color.rgb(151,167,184), accent=Color.rgb(88,228,218);
    public SongAdapter(OnSongClick listener){click=listener;}
    public void submit(List<Song> list,String activeId){
        songs.clear();songs.addAll(list);currentId=activeId==null?"":activeId;
        notifyDataSetChanged();
    }
    public int getItemCount(){return songs.size();}
    @Override public Holder onCreateViewHolder(ViewGroup parent,int type){
        android.content.Context c=parent.getContext();
        float density=c.getResources().getDisplayMetrics().density;
        LinearLayout container=new LinearLayout(c);
        container.setGravity(Gravity.CENTER_VERTICAL);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setPadding((int)(12*density),(int)(9*density),(int)(12*density),(int)(9*density));
        TextView symbol=new TextView(c);symbol.setText("♫");symbol.setTextSize(23);
        symbol.setTextColor(accent);symbol.setGravity(Gravity.CENTER);
        GradientDrawable tile=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xff1f3b48,0xff142433}); tile.setCornerRadius(15*density);
        symbol.setBackground(tile);
        container.addView(symbol,new LinearLayout.LayoutParams((int)(54*density),(int)(54*density)));
        LinearLayout names=new LinearLayout(c);names.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams n=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1);
        n.setMargins((int)(14*density),0,(int)(4*density),0);
        container.addView(names,n);
        TextView title=new TextView(c);title.setTextSize(15);title.setTextColor(light);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setMaxLines(1);title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        names.addView(title);
        TextView sub=new TextView(c);sub.setTextSize(12);sub.setTextColor(dim);
        sub.setMaxLines(1);sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=(int)(4*density);
        names.addView(sub,sp);
        TextView menu=new TextView(c);menu.setText("›");menu.setTextColor(dim);menu.setTextSize(30);menu.setGravity(Gravity.CENTER);
        container.addView(menu,new LinearLayout.LayoutParams((int)(25*density),-2));
        return new Holder(container,symbol,title,sub);
    }
    @Override public void onBindViewHolder(Holder h,int pos){
        Song s=songs.get(pos);boolean playing=s.uri.toString().equals(currentId);
        h.symbol.setText(playing?"▶":"♫");
        h.title.setText(s.title);
        h.title.setTextColor(playing?accent:light);
        h.sub.setText(s.artist+"  ·  "+s.extension);
        h.itemView.setOnClickListener(v->click.open(s));
    }
    static class Holder extends RecyclerView.ViewHolder {
        final TextView symbol,title,sub;
        Holder(View v,TextView a,TextView b,TextView c){super(v);symbol=a;title=b;sub=c;}
    }
}
