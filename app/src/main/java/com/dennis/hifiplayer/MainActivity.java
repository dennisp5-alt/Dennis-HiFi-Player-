package com.dennis.hifiplayer;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.LruCache;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    private static final int PICK_MUSIC_DIRECTORY=2201;
    private static final int INK=0xff09111D, SURFACE=0xff142333, PANEL=0xff192C3C;
    private static final int WHITE=0xffF2F6F8, MUTED=0xff9AACBD, TEAL=0xff58E4DA, GOLD=0xffD4B783;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService covers=Executors.newSingleThreadExecutor();
    private final LruCache<String,Bitmap> artwork=new LruCache<>(12);
    private LibraryScanner scanner;
    private AppPrefs prefs;
    private ListenableFuture<MediaController> controllerFuture;
    private MediaController controller;
    private final List<Song> allSongs=new ArrayList<>(), visibleSongs=new ArrayList<>();
    private String folderFilter="",genreFilter="",search="",tab="LIBRARY";
    private LinearLayout outer,toolbar,mini,folderRow,genreRow,libraryView,soundView,nowView,modeButtons,levels;
    private FrameLayout page;
    private TextView libCount,folderDescription,miniTitle,miniArtist,playMini,nowTitle,nowArtist,
            playBig,progressText,remainingText,autoDescription,boosterText,repeatLabel,shuffleLabel,
            nowGenre,presetPicker,folderGenreButton;
    private ImageView coverArt,miniArt;
    private SeekBar progress,booster;
    private final TextView[] modeChips=new TextView[4];
    private final SeekBar[] eqSliders=new SeekBar[5];
    private final TextView[] eqNumbers=new TextView[5];
    private SongAdapter adapter;
    private boolean scrubbing=false,shuffle=false;
    private final Player.Listener playerListener=new Player.Listener(){
        @Override public void onEvents(Player player,Player.Events events) { updatePlaybackUi(); }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(INK);getWindow().setNavigationBarColor(INK);
        getWindow().getDecorView().setSystemUiVisibility(0);
        prefs=new AppPrefs(this);scanner=new LibraryScanner(this);
        renderShell();buildLibrary();buildSound();buildNowPlaying();setTab("LIBRARY");
        loadLibrary();connectSession();
        handler.postDelayed(clock,500);
    }
    private void connectSession(){
        controllerFuture=new MediaController.Builder(this,new SessionToken(this,new ComponentName(this,PlaybackService.class))).buildAsync();
        controllerFuture.addListener(()->{
            try {controller=controllerFuture.get();controller.addListener(playerListener);updatePlaybackUi();}
            catch(Exception e){toast("Playback connection unavailable: "+e.getMessage());}
        },getMainExecutor());
    }
    private final Runnable clock=new Runnable(){@Override public void run(){
        if(controller!=null && controller.isPlaying())updateTime();
        handler.postDelayed(this,550);
    }};
    @Override protected void onDestroy(){
        handler.removeCallbacks(clock);
        if(controller!=null)controller.removeListener(playerListener);
        if(controllerFuture!=null)MediaController.releaseFuture(controllerFuture);
        scanner.close();covers.shutdownNow();super.onDestroy();
    }
    private void renderShell(){
        outer=new LinearLayout(this);outer.setOrientation(LinearLayout.VERTICAL);outer.setBackgroundColor(INK);
        outer.setPadding(dp(17),dp(10),dp(17),dp(7));
        // Android 15/16 edge-to-edge: keep toolbar and mini player clear of system bars.
        outer.setOnApplyWindowInsetsListener((view,insets)->{
            int top,bottom;
            if(Build.VERSION.SDK_INT>=30){
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
                top=bars.top;bottom=bars.bottom;
            }else{
                top=insets.getSystemWindowInsetTop();bottom=insets.getSystemWindowInsetBottom();
            }
            view.setPadding(dp(17),top+dp(10),dp(17),bottom+dp(7));
            return insets;
        });
        setContentView(outer);
        toolbar=new LinearLayout(this);toolbar.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout brand=new LinearLayout(this);brand.setOrientation(LinearLayout.VERTICAL);
        toolbar.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        TextView eyebrow=txt("DENNIS  /  AUDIO",11,GOLD,true);eyebrow.setLetterSpacing(.2f);brand.addView(eyebrow);
        TextView heading=txt("Hi-Fi Player",28,WHITE,true);
        brand.addView(heading);
        TextView add=txt("+ FOLDER",12,TEAL,true);add.setGravity(Gravity.CENTER);
        add.setBackground(panelDrawable(SURFACE,16));add.setPadding(dp(15),dp(15),dp(15),dp(15));
        add.setOnClickListener(v->pickFolder()); toolbar.addView(add);
        outer.addView(toolbar);
        TextView subtitle=txt("YOUR MUSIC.  YOUR SOUND.  OFFLINE.",10,MUTED,false);
        subtitle.setLetterSpacing(.1f);LinearLayout.LayoutParams subLp=space(-1,-2,dp(5),dp(13));outer.addView(subtitle,subLp);
        LinearLayout tabs=new LinearLayout(this);tabs.setGravity(Gravity.CENTER);
        tabs.setBackground(panelDrawable(SURFACE,15));tabs.setPadding(dp(4),dp(4),dp(4),dp(4));
        for(String which:new String[]{"LIBRARY","NOW PLAYING","SOUND"}){
            TextView v=txt(which,11,TEAL,true);v.setGravity(Gravity.CENTER);
            v.setPadding(dp(4),dp(13),dp(4),dp(13));
            tabs.addView(v,new LinearLayout.LayoutParams(0,-2,1));v.setTag(which);
            v.setOnClickListener(w->setTab((String)w.getTag()));
        }
        outer.addView(tabs);
        page=new FrameLayout(this);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,0,1);p.topMargin=dp(14);outer.addView(page,p);
        mini=new LinearLayout(this);mini.setGravity(Gravity.CENTER_VERTICAL);mini.setPadding(dp(8),dp(8),dp(12),dp(8));
        mini.setBackground(panelDrawable(PANEL,17));
        LinearLayout.LayoutParams m=space(-1,dp(67),0,dp(4));outer.addView(mini,m);
        miniArt=new ImageView(this);miniArt.setScaleType(ImageView.ScaleType.CENTER_CROP);
        miniArt.setImageResource(com.dennis.hifiplayer.R.drawable.default_art);
        mini.addView(miniArt,new LinearLayout.LayoutParams(dp(50),dp(50)));
        LinearLayout details=new LinearLayout(this);details.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams d=new LinearLayout.LayoutParams(0,-2,1);d.leftMargin=dp(13);mini.addView(details,d);
        miniTitle=txt("",13,WHITE,true);oneLine(miniTitle);details.addView(miniTitle);
        miniArtist=txt("",11,MUTED,false);oneLine(miniArtist);details.addView(miniArtist);
        playMini=txt("▶",27,TEAL,true);playMini.setGravity(Gravity.CENTER);
        mini.addView(playMini,new LinearLayout.LayoutParams(dp(46),dp(48)));
        playMini.setOnClickListener(v->togglePlayback());
        details.setOnClickListener(v->setTab("NOW PLAYING"));
        miniArt.setOnClickListener(v->setTab("NOW PLAYING"));mini.setVisibility(View.GONE);
    }
    private void setTab(String t){
        tab=t;page.removeAllViews();View destination;
        if("SOUND".equals(t)) destination=soundView;
        else if("NOW PLAYING".equals(t)) destination=nowView;
        else destination=libraryView;
        if(destination!=null){ViewGroup parent=(ViewGroup)destination.getParent();if(parent!=null)parent.removeView(destination);
            page.addView(destination,new FrameLayout.LayoutParams(-1,-1));}
        // Highlight active tab.
        View tabs=outer.getChildAt(2);
        if(tabs instanceof LinearLayout)for(int i=0;i<((LinearLayout)tabs).getChildCount();i++){
            TextView chip=(TextView)((LinearLayout)tabs).getChildAt(i);
            boolean active=t.equals(chip.getTag());
            chip.setTextColor(active?INK:MUTED);
            chip.setBackground(active?panelDrawable(TEAL,12):null);
        }
        updatePlaybackUi();
    }
    private void buildLibrary(){
        libraryView=new LinearLayout(this);libraryView.setOrientation(LinearLayout.VERTICAL);
        TextView intro=txt("The collection",22,WHITE,true);
        libraryView.addView(intro);
        libCount=txt("Choose a folder to begin",12,MUTED,false);
        libraryView.addView(libCount,space(-1,-2,0,dp(10)));
        EditText searchBox=new EditText(this);searchBox.setSingleLine(true);
        searchBox.setTextColor(WHITE);searchBox.setHintTextColor(MUTED);searchBox.setTextSize(14);
        searchBox.setHint("Search tracks or artists");searchBox.setPadding(dp(16),0,dp(16),0);
        searchBox.setBackground(panelDrawable(SURFACE,14));
        libraryView.addView(searchBox,space(-1,dp(48),0,dp(12)));
        searchBox.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int a,int c,int f){}
            public void onTextChanged(CharSequence s,int st,int before,int count){ search=s.toString().toLowerCase(Locale.ROOT).trim();applyFilter(); }
            public void afterTextChanged(Editable e){}
        });
        folderDescription=txt("FOLDERS",10,GOLD,true);folderDescription.setLetterSpacing(.15f);
        libraryView.addView(folderDescription);
        HorizontalScrollView folderScroll=new HorizontalScrollView(this);folderScroll.setHorizontalScrollBarEnabled(false);
        folderRow=new LinearLayout(this);folderRow.setGravity(Gravity.CENTER_VERTICAL);
        folderScroll.addView(folderRow);libraryView.addView(folderScroll,space(-1,dp(55),0,dp(4)));
        folderGenreButton=txt("ASSIGN GENRE TO SELECTED FOLDER  ›",11,TEAL,true);
        folderGenreButton.setPadding(dp(8),dp(6),dp(8),dp(11));
        folderGenreButton.setOnClickListener(v->{if(!folderFilter.isEmpty())editFolderGenre(folderFilter);});
        libraryView.addView(folderGenreButton);
        TextView genreHeader=txt("GENRES  ·  FILTER YOUR MUSIC",10,GOLD,true);
        libraryView.addView(genreHeader);
        HorizontalScrollView genreScroll=new HorizontalScrollView(this);
        genreScroll.setHorizontalScrollBarEnabled(false);
        genreRow=new LinearLayout(this);genreRow.setGravity(Gravity.CENTER_VERTICAL);
        genreScroll.addView(genreRow);libraryView.addView(genreScroll,space(-1,dp(52),0,dp(5)));
        adapter=new SongAdapter(this::playSong);
        RecyclerView list=new RecyclerView(this);list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);list.setClipToPadding(false);list.setPadding(0,0,0,dp(8));
        libraryView.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        refreshFolders();
    }
    private void refreshFolders(){
        if(folderRow==null)return;
        folderRow.removeAllViews();folderChip("All folders",folderFilter.isEmpty(),()->{folderFilter="";genreFilter="";refreshGenres();applyFilter();refreshFolders();},null);
        for(String folder:prefs.folders()){
            String label=folderLabel(folder);
            folderChip(label,folder.equals(folderFilter),()->{folderFilter=folder;genreFilter="";refreshGenres();applyFilter();refreshFolders();},()->confirmRemove(folder,label));
        }
        folderDescription.setText("FOLDERS  ·  TAP TO FILTER  ·  HOLD TO REMOVE");
        if(folderGenreButton!=null)folderGenreButton.setVisibility(folderFilter.isEmpty()?View.GONE:View.VISIBLE);
    }
    private void folderChip(String name,boolean active,Runnable select,Runnable remove){
        TextView chip=txt(name,12,active?INK:MUTED,active);
        chip.setPadding(dp(15),dp(10),dp(15),dp(10));chip.setBackground(panelDrawable(active?TEAL:SURFACE,13));
        LinearLayout.LayoutParams cp=space(-2,-2,0,0);cp.rightMargin=dp(8);folderRow.addView(chip,cp);
        chip.setOnClickListener(v->select.run());
        if(remove!=null)chip.setOnLongClickListener(v->{remove.run();return true;});
    }
    private String folderLabel(String uri){
        try {
            String doc=DocumentsContract.getTreeDocumentId(Uri.parse(uri));
            String[] p=doc.split(":",2);String leaf=p.length==2?p[1]:p[0];
            if(leaf.contains("/"))leaf=leaf.substring(leaf.lastIndexOf('/')+1);
            return leaf.isEmpty()?"Music":leaf;
        }catch(Exception e){return "Music folder";}
    }
    private void confirmRemove(String uri,String name){
        new AlertDialog.Builder(this).setTitle("Remove "+name+"?")
                .setMessage("Only removes this folder from the player. Your music files stay untouched.")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Remove",(d,w)->{
                    prefs.removeFolder(uri);if(uri.equals(folderFilter))folderFilter="";
                    try{getContentResolver().releasePersistableUriPermission(Uri.parse(uri),Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
                    refreshFolders();refreshGenres();loadLibrary();
                }).show();
    }
    private void refreshGenres(){
        if(genreRow==null)return;
        genreRow.removeAllViews();
        genreChip("All genres","",genreFilter.isEmpty());
        Set<String> present=new LinkedHashSet<>();
        for(Song song:allSongs){
            if(!folderFilter.isEmpty() && !folderFilter.equals(song.rootUri))continue;
            present.add(AutoProfile.classifyId(prefs.effectiveGenre(song)));
        }
        for(String id:AutoProfile.IDS){
            if(present.contains(id))genreChip(AutoProfile.preset(id).name,id,id.equals(genreFilter));
        }
        if(present.contains("neutral"))genreChip("Uncategorised","neutral","neutral".equals(genreFilter));
    }
    private void genreChip(String label,String id,boolean active){
        TextView chip=txt(label,11,active?INK:MUTED,active);
        chip.setPadding(dp(14),dp(9),dp(14),dp(9));chip.setBackground(panelDrawable(active?TEAL:SURFACE,12));
        LinearLayout.LayoutParams lp=space(-2,-2,0,0);lp.rightMargin=dp(7);genreRow.addView(chip,lp);
        chip.setOnClickListener(v->{genreFilter=id;refreshGenres();applyFilter();});
    }
    private interface GenreSelected {void select(String id);}
    private void showGenreDialog(String selected,boolean presetOnly,GenreSelected action){
        String[] labels=new String[AutoProfile.NAMES.length+(presetOnly?0:1)];
        int start=presetOnly?0:1;
        if(!presetOnly)labels[0]="Use embedded genre / clear assignment";
        System.arraycopy(AutoProfile.NAMES,0,labels,start,AutoProfile.NAMES.length);
        int active=presetOnly?0:-1;
        for(int i=0;i<AutoProfile.IDS.length;i++)if(AutoProfile.IDS[i].equals(selected))active=i+start;
        new AlertDialog.Builder(this).setTitle(presetOnly?"Choose listening EQ preset":"Assign music genre")
            .setSingleChoiceItems(labels,active,(dialog,which)->{
                dialog.dismiss();action.select(which<start?"":AutoProfile.IDS[which-start]);
            }).setNegativeButton("Cancel",null).show();
    }
    private void editCurrentTrackGenre(){
        Song song=currentSong();
        if(song==null){toast("Select a song first");return;}
        showGenreDialog(prefs.trackGenre(song.uri.toString()),false,id->{
            prefs.trackGenre(song.uri.toString(),id);
            refreshGenres();applyFilter();notifyFx();updatePlaybackUi();
        });
    }
    private void editFolderGenre(String folder){
        showGenreDialog(prefs.folderGenre(folder),false,id->{
            prefs.folderGenre(folder,id);
            refreshGenres();applyFilter();notifyFx();updatePlaybackUi();
        });
    }
    private void pickFolder(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i,PICK_MUSIC_DIRECTORY);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==PICK_MUSIC_DIRECTORY&&result==RESULT_OK&&data!=null&&data.getData()!=null){
            Uri uri=data.getData();
            try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}
            catch(SecurityException e){toast("Cannot retain folder access");return;}
            prefs.addFolder(uri.toString());refreshFolders();loadLibrary();
        }
    }
    private void loadLibrary(){
        if(libCount!=null)libCount.setText("Scanning selected music folders…");
        scanner.scan(prefs.folders(),(songs,unavailable,truncated)->runOnUiThread(()->{
            allSongs.clear();allSongs.addAll(songs);
            refreshGenres();applyFilter();
            if(!unavailable.isEmpty())toast(unavailable.size()+" folder(s) unavailable. Re-select them if moved.");
            if(truncated)toast("12,000-track scan limit reached; narrow your folders.");
        }));
    }
    private void applyFilter(){
        visibleSongs.clear();
        for(Song s:allSongs){
            if(!folderFilter.isEmpty() && !folderFilter.equals(s.rootUri))continue;
            if(!genreFilter.isEmpty() && !genreFilter.equals(AutoProfile.classifyId(prefs.effectiveGenre(s))))continue;
            if(!search.isEmpty() && !(s.title+" "+s.artist+" "+s.album+" "+prefs.effectiveGenre(s)).toLowerCase(Locale.ROOT).contains(search))continue;
            visibleSongs.add(s);
        }
        String id=currentId();
        if(adapter!=null)adapter.submit(visibleSongs,id);
        if(libCount!=null)libCount.setText(visibleSongs.size()+" tracks   ·   "+prefs.folders().size()+" music folders");
    }
    private void playSong(Song track){
        if(controller==null){toast("Player is connecting. Try again.");return;}
        if(android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},36);
        List<MediaItem> items=new ArrayList<>();int start=0;
        for(int i=0;i<visibleSongs.size();i++){
            Song s=visibleSongs.get(i);items.add(s.asMediaItem(prefs.effectiveGenre(s)));if(s.uri.equals(track.uri))start=i;
        }
        if(items.isEmpty())return;
        controller.setMediaItems(items,start,0L);controller.prepare();controller.play();
        setTab("NOW PLAYING");updatePlaybackUi();
    }
    private void buildNowPlaying(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        nowView=new LinearLayout(this);nowView.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(nowView);
        LinearLayout wrapper=new LinearLayout(this);wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(scroll,new LinearLayout.LayoutParams(-1,-1));
        nowView=wrapper;
        // Add actual player children to the scroll's content, not the wrapper.
        LinearLayout body=(LinearLayout)scroll.getChildAt(0);
        TextView nowTag=txt("CURRENTLY PLAYING",11,GOLD,true);nowTag.setLetterSpacing(.2f);
        body.addView(nowTag,space(-1,-2,0,dp(15)));
        coverArt=new ImageView(this);coverArt.setImageResource(R.drawable.default_art);
        coverArt.setScaleType(ImageView.ScaleType.CENTER_CROP);
        int size=Math.min(getResources().getDisplayMetrics().widthPixels-dp(48),dp(330));
        LinearLayout.LayoutParams art=space(size,size,0,dp(21));art.gravity=Gravity.CENTER_HORIZONTAL;
        body.addView(coverArt,art);
        nowTitle=txt("Choose a track",23,WHITE,true);nowTitle.setGravity(Gravity.CENTER);nowTitle.setMaxLines(2);
        body.addView(nowTitle);
        nowArtist=txt("Select a music folder to begin",14,MUTED,false);nowArtist.setGravity(Gravity.CENTER);
        body.addView(nowArtist,space(-1,-2,0,dp(7)));
        nowGenre=txt("GENRE  ·  TAP TO SET",12,TEAL,true);
        nowGenre.setGravity(Gravity.CENTER);nowGenre.setPadding(dp(8),dp(7),dp(8),dp(16));
        nowGenre.setOnClickListener(v->editCurrentTrackGenre());
        body.addView(nowGenre,space(-1,-2,0,dp(7)));
        progress=new SeekBar(this);progress.setMax(1000);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(TEAL));
        progress.setThumbTintList(android.content.res.ColorStateList.valueOf(TEAL));
        body.addView(progress,new LinearLayout.LayoutParams(-1,dp(39)));
        progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar b){scrubbing=true;}
            public void onProgressChanged(SeekBar b,int p,boolean user){
                if(user&&controller!=null&&controller.getDuration()>0)progressText.setText(time(controller.getDuration()*p/1000));
            }
            public void onStopTrackingTouch(SeekBar b){
                if(controller!=null&&controller.getDuration()>0)controller.seekTo(controller.getDuration()*b.getProgress()/1000);
                scrubbing=false;
            }
        });
        LinearLayout times=new LinearLayout(this);progressText=txt("0:00",11,MUTED,false);
        remainingText=txt("0:00",11,MUTED,false);times.addView(progressText,new LinearLayout.LayoutParams(0,-2,1));times.addView(remainingText);
        body.addView(times);
        LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);
        shuffleLabel=control(controls,"⇄",30,()->{
            if(controller!=null){controller.setShuffleModeEnabled(!controller.getShuffleModeEnabled());updatePlaybackUi();}
        });
        control(controls,"|◀",25,()->{if(controller!=null)controller.seekToPreviousMediaItem();});
        playBig=control(controls,"▶",35,this::togglePlayback);
        playBig.setTextColor(INK);playBig.setBackground(panelDrawable(TEAL,99));
        control(controls,"▶|",25,()->{if(controller!=null)controller.seekToNextMediaItem();});
        repeatLabel=control(controls,"↻",28,()->{
            if(controller==null)return;
            int repeat=controller.getRepeatMode();
            controller.setRepeatMode(repeat==Player.REPEAT_MODE_OFF?Player.REPEAT_MODE_ALL:
                    repeat==Player.REPEAT_MODE_ALL?Player.REPEAT_MODE_ONE:Player.REPEAT_MODE_OFF);
            updatePlaybackUi();
        });
        body.addView(controls,space(-1,dp(95),0,dp(11)));
        TextView soundLink=txt("TUNE YOUR SOUND    ›",12,TEAL,true);soundLink.setGravity(Gravity.CENTER);
        soundLink.setPadding(dp(12),dp(16),dp(12),dp(16));soundLink.setBackground(panelDrawable(SURFACE,16));
        soundLink.setOnClickListener(v->setTab("SOUND"));body.addView(soundLink);
        TextView hint=txt("Original playback when EQ and boost are off. Audio output quality also depends on your device and connection.",12,MUTED,false);
        hint.setGravity(Gravity.CENTER);body.addView(hint,space(-1,-2,0,dp(20)));
    }
    private TextView control(LinearLayout row,String title,int size,Runnable action){
        TextView b=txt(title,size,WHITE,true);b.setGravity(Gravity.CENTER);
        row.addView(b,new LinearLayout.LayoutParams(0,dp(75),1));b.setOnClickListener(v->action.run());return b;
    }
    private void buildSound(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setVerticalScrollBarEnabled(false);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(body);soundView=new LinearLayout(this);soundView.setOrientation(LinearLayout.VERTICAL);
        soundView.addView(scroll,new LinearLayout.LayoutParams(-1,-1));
        TextView header=txt("Sound studio",25,WHITE,true);body.addView(header);
        TextView subtitle=txt("Fine-tune your listening without changing the source files.",13,MUTED,false);
        body.addView(subtitle,space(-1,-2,0,dp(25)));
        body.addView(txt("EQUALIZER",11,GOLD,true));
        modeButtons=new LinearLayout(this);modeButtons.setGravity(Gravity.CENTER);
        modeButtons.setPadding(dp(4),dp(4),dp(4),dp(4));modeButtons.setBackground(panelDrawable(SURFACE,14));
        for(int i=0;i<4;i++){
            final String mode=new String[]{"off","auto","preset","manual"}[i];
            TextView chip=txt(mode.toUpperCase(Locale.ROOT),12,MUTED,true);chip.setGravity(Gravity.CENTER);
            chip.setPadding(0,dp(15),0,dp(15));modeButtons.addView(chip,new LinearLayout.LayoutParams(0,-2,1));
            chip.setOnClickListener(v->{prefs.mode(mode);refreshSoundUi();notifyFx();});
            modeChips[i]=chip;
        }
        body.addView(modeButtons,space(-1,-2,0,dp(16)));
        autoDescription=txt("",13,MUTED,false);autoDescription.setPadding(dp(15),dp(15),dp(15),dp(15));
        autoDescription.setBackground(panelDrawable(PANEL,14));
        body.addView(autoDescription,space(-1,-2,0,dp(13)));
        presetPicker=txt("CHOOSE GENRE PRESET  ›",13,TEAL,true);
        presetPicker.setPadding(dp(17),dp(14),dp(17),dp(14));
        presetPicker.setBackground(panelDrawable(PANEL,13));
        presetPicker.setOnClickListener(v->showGenreDialog(prefs.preset(),true,(id)->{
            prefs.preset(id);prefs.mode("preset");refreshSoundUi();notifyFx();
        }));
        body.addView(presetPicker,space(-1,-2,0,dp(16)));
        levels=new LinearLayout(this);levels.setOrientation(LinearLayout.VERTICAL);
        String[] labels={"BASS   80 Hz","LOW MID   250 Hz","MID   1 kHz","PRESENCE   4 kHz","AIR   12 kHz"};
        for(int i=0;i<5;i++){
            int idx=i;
            LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);
            TextView label=txt(labels[i],11,WHITE,true);line.addView(label,new LinearLayout.LayoutParams(0,-2,1));
            TextView number=txt("0 dB",12,TEAL,true);eqNumbers[i]=number;line.addView(number);
            levels.addView(line,space(-1,-2,0,dp(5)));
            SeekBar slider=new SeekBar(this);slider.setMax(12);slider.setProgress(prefs.gain(i)+6);
            slider.setProgressTintList(android.content.res.ColorStateList.valueOf(TEAL));
            slider.setThumbTintList(android.content.res.ColorStateList.valueOf(GOLD));
            eqSliders[i]=slider;
            levels.addView(slider,space(-1,dp(39),0,dp(9)));
            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                public void onProgressChanged(SeekBar v,int progress,boolean fromUser){
                    if(fromUser){prefs.gain(idx,progress-6);eqNumbers[idx].setText((progress-6>0?"+":"")+(progress-6)+" dB");notifyFx();}
                }
                public void onStartTrackingTouch(SeekBar v){}
                public void onStopTrackingTouch(SeekBar v){}
            });
        }
        body.addView(levels);
        TextView reset=txt("RESET MANUAL EQ TO FLAT",12,TEAL,true);reset.setGravity(Gravity.END);
        reset.setPadding(0,dp(8),0,dp(22));
        reset.setOnClickListener(v->{for(int i=0;i<5;i++)prefs.gain(i,0);refreshSoundUi();notifyFx();});
        body.addView(reset);
        body.addView(txt("CLEAN VOLUME LIFT",11,GOLD,true));
        boosterText=txt("",14,WHITE,true);body.addView(boosterText,space(-1,-2,0,dp(9)));
        booster=new SeekBar(this);booster.setMax(12);booster.setProgress(prefs.boost());
        booster.setProgressTintList(android.content.res.ColorStateList.valueOf(TEAL));
        booster.setThumbTintList(android.content.res.ColorStateList.valueOf(GOLD));
        body.addView(booster);
        booster.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean user){
                if(user){prefs.boost(p);updateBoosterLabel();notifyFx();}
            }
            public void onStartTrackingTouch(SeekBar s){}
            public void onStopTrackingTouch(SeekBar s){}
        });
        TextView caution=txt("Boost reaches +6 dB in 0.5 dB steps using Android loudness processing. Loud recordings can compress or distort at higher settings; this is not a true-peak limiter. For maximum fidelity, keep boost off. Protect your hearing.",12,MUTED,false);
        caution.setPadding(0,dp(9),0,dp(25));body.addView(caution);
        refreshSoundUi();
    }
    private void updateBoosterLabel(){
        int n=prefs.boost();if(boosterText!=null)boosterText.setText(n==0?"Off · Original level":"On · +"+(n/2f)+" dB maximum gain");
    }
    private void refreshSoundUi(){
        String mode=prefs.mode();
        String[] modes={"off","auto","preset","manual"};
        for(int i=0;i<4;i++){
            boolean active=mode.equals(modes[i]);
            modeChips[i].setBackground(active?panelDrawable(TEAL,12):null);
            modeChips[i].setTextColor(active?INK:MUTED);
        }
        boolean manual=mode.equals("manual");
        AutoProfile preset=AutoProfile.preset(prefs.preset());
        Song song=currentSong();
        AutoProfile detected=AutoProfile.classify(song==null?"":prefs.effectiveGenre(song));
        for(int i=0;i<5;i++){
            eqSliders[i].setEnabled(manual);eqSliders[i].setAlpha(manual?1f:.45f);
            float gain=manual?prefs.gain(i):mode.equals("preset")?preset.gains[i]:mode.equals("auto")?detected.gains[i]:0;
            eqSliders[i].setProgress(Math.max(0,Math.min(12,Math.round(gain)+6)));
            eqNumbers[i].setText(String.format(Locale.ROOT,"%+.1f dB",gain));
        }
        presetPicker.setVisibility(mode.equals("preset")?View.VISIBLE:View.GONE);
        presetPicker.setText("CHOOSE GENRE  ·  "+preset.name+"  ›");
        if(mode.equals("auto")){
            autoDescription.setText("AUTO PROFILE  ·  "+detected.name+"\nReads embedded genre tags or your track/folder assignments. Does not analyse the audio signal. Unknown genres stay neutral.");
        } else if(mode.equals("preset"))autoDescription.setText("FIXED GENRE PRESET  ·  "+preset.name+"\nThis EQ stays active for every song until you change it.");
        else if(mode.equals("manual"))autoDescription.setText("MANUAL EQ\nYour five-band EQ is applied to every song.");
        else autoDescription.setText("DIRECT LISTENING\nEQ is disabled; no tonal adjustment from this app.");
        booster.setProgress(prefs.boost());updateBoosterLabel();
    }
    private void notifyFx(){
        Intent refresh=new Intent(PlaybackService.FX_REFRESH).setPackage(getPackageName());
        Song song=currentSong();
        if(song!=null){refresh.putExtra("media_id",song.uri.toString());refresh.putExtra("genre",prefs.effectiveGenre(song));}
        sendBroadcast(refresh);
    }
    private void togglePlayback(){if(controller==null)return;
        if(controller.isPlaying())controller.pause();else{
            if(controller.getMediaItemCount()>0){controller.prepare();controller.play();}
            else setTab("LIBRARY");
        }
        updatePlaybackUi();
    }
    private String currentId(){
        if(controller==null||controller.getCurrentMediaItem()==null)return "";
        return controller.getCurrentMediaItem().mediaId;
    }
    private Song currentSong(){String id=currentId();for(Song s:allSongs)if(s.uri.toString().equals(id))return s;return null;}
    private void updatePlaybackUi(){
        if(nowTitle==null||controller==null)return;
        MediaItem item=controller.getCurrentMediaItem();
        boolean has=item!=null;
        mini.setVisibility(has?View.VISIBLE:View.GONE);
        if(!has)return;
        Song song=currentSong();
        CharSequence title=item.mediaMetadata.title;CharSequence artist=item.mediaMetadata.artist;
        nowTitle.setText(title==null?"Untitled":title);
        nowArtist.setText(artist==null?"Unknown artist":artist);
        miniTitle.setText(nowTitle.getText());miniArtist.setText(nowArtist.getText());
        if(nowGenre!=null)nowGenre.setText("GENRE  ·  "+(song==null?"UNKNOWN":AutoProfile.classify(prefs.effectiveGenre(song)).name.toUpperCase(Locale.ROOT))+"  ·  TAP TO EDIT");
        boolean playing=controller.isPlaying();playBig.setText(playing?"Ⅱ":"▶");playMini.setText(playing?"Ⅱ":"▶");
        shuffleLabel.setTextColor(controller.getShuffleModeEnabled()?TEAL:MUTED);
        repeatLabel.setText(controller.getRepeatMode()==Player.REPEAT_MODE_ONE?"↻¹":"↻");
        repeatLabel.setTextColor(controller.getRepeatMode()==Player.REPEAT_MODE_OFF?MUTED:TEAL);
        applyCover(song,coverArt);applyCover(song,miniArt);
        if("SOUND".equals(tab))refreshSoundUi();
        if(adapter!=null)adapter.submit(visibleSongs,currentId());
        updateTime();
    }
    private void updateTime(){
        if(controller==null||progress==null)return;
        long duration=controller.getDuration(),position=controller.getCurrentPosition();
        if(duration<=0)duration=0;if(position<0)position=0;
        if(!scrubbing){progress.setProgress(duration>0?(int)Math.min(1000,position*1000/duration):0);progressText.setText(time(position));}
        remainingText.setText(duration>0?"-"+time(Math.max(0,duration-position)):"—:—");
    }
    private String time(long ms){long s=Math.max(0,ms/1000);return (s/60)+":"+String.format(Locale.ROOT,"%02d",s%60);}
    private void applyCover(Song s, ImageView target){
        if(target==null)return;
        String id=s==null?"":s.uri.toString();
        if(id.equals(target.getTag()))return;
        target.setTag(id);
        if(s==null){target.setImageResource(R.drawable.default_art);return;}
        Bitmap cached=artwork.get(id);
        if(cached!=null){target.setImageBitmap(cached);return;}
        target.setImageResource(R.drawable.default_art);
        covers.submit(()->{
            MediaMetadataRetriever r=new MediaMetadataRetriever();Bitmap bmp=null;
            try {
                r.setDataSource(getApplicationContext(),s.uri);
                byte[] bytes=r.getEmbeddedPicture();
                if(bytes!=null && bytes.length<12_000_000){
                    BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;
                    BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);
                    o.inSampleSize=1;while(o.outWidth/o.inSampleSize>640||o.outHeight/o.inSampleSize>640)o.inSampleSize*=2;
                    o.inJustDecodeBounds=false;bmp=BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);
                }
            }catch(Exception ignored){}finally{try{r.release();}catch(Exception ignored){}}
            if(bmp!=null){Bitmap actual=bmp;artwork.put(id,actual);
                runOnUiThread(()->{
                    if(id.equals(coverArt.getTag()))coverArt.setImageBitmap(actual);
                    if(id.equals(miniArt.getTag()))miniArt.setImageBitmap(actual);
                });
            }
        });
    }
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private LinearLayout.LayoutParams space(int w,int h,int top,int bottom){
        LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(w,h);a.topMargin=top;a.bottomMargin=bottom;return a;
    }
    private TextView txt(String value,int sp,int color,boolean bold){
        TextView v=new TextView(this);v.setText(value);v.setTextColor(color);v.setTextSize(sp);
        if(bold)v.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));return v;
    }
    private GradientDrawable panelDrawable(int color,int radius){
        GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;
    }
    private void oneLine(TextView v){v.setSingleLine(true);v.setEllipsize(android.text.TextUtils.TruncateAt.END);}
}
