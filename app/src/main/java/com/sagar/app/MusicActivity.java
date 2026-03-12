package com.sagar.app;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.view.ViewGroup;
import android.graphics.Color;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.HashMap;
import java.util.Map;

public class MusicActivity extends AppCompatActivity {

    private android.view.ViewGroup bottomNavMenu;
    private android.widget.LinearLayout navHome, navPlanner, navMusic, navAbout;
    private android.widget.ImageView ivHome, ivPlanner, ivMusic, ivAbout;
    private TextView tvHomeLabel, tvPlannerLabel, tvMusicLabel, tvAboutLabel;
    private ImageView ivFeaturedImage, ivForestItem, ivMountainsItem, ivDesertItem, ivHillItem;

    // Optimized Image URLs for performance
    private static final String URL_FEATURED = "https://images.unsplash.com/photo-1534274988757-a28bf1f539cc?q=80&w=600";
    private static final String URL_FOREST = "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?q=80&w=300";
    private static final String URL_MOUNTAINS = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=300";
    private static final String URL_DESERT = "https://images.unsplash.com/photo-1473580044384-7ba9967e16a0?q=80&w=300";
    private static final String URL_HILL = "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?q=80&w=300";

    // Real MediaPlayer persistent state
    private static MediaPlayer mediaPlayer;
    private static int currentTrackIndex = -1;
    private static boolean isPlaying = false;

    private final String[] TRACK_TITLES = {
            "Gentle Rain", "Forest Morning", "Arctic Wind", "Night Crickets", "Mountain River"
    };
    private final String[] TRACK_SUBTITLES = {
            "Ambient Focus", "Nature Scene", "Deep Concentration", "Calm Evening", "Flow State"
    };
    private final int[] TRACK_IMAGES = {
            R.drawable.forest, R.drawable.green_forest, R.drawable.mountains, R.drawable.desert, R.drawable.hill
    };

    private Handler seekHandler = new Handler(Looper.getMainLooper());
    private Runnable seekRunnable;

    // Reliable nature-sound streaming URLs (Defaults switched to Local Raw for
    // speed)
    private String[] dynamicStreamUrls = {
            "android.resource://com.sagar.app/" + R.raw.gentle_rain,
            "android.resource://com.sagar.app/" + R.raw.forest_morning,
            "android.resource://com.sagar.app/" + R.raw.arctic_wind,
            "android.resource://com.sagar.app/" + R.raw.night_craker,
            "android.resource://com.sagar.app/" + R.raw.mountain_river
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

        bottomNavMenu = findViewById(R.id.bottomNavMenu);
        navHome = findViewById(R.id.navHome);
        navPlanner = findViewById(R.id.navPlanner);
        navMusic = findViewById(R.id.navMusic);
        navAbout = findViewById(R.id.navAbout);
        navAbout = findViewById(R.id.navAbout);

        ivHome = findViewById(R.id.ivHome);
        ivPlanner = findViewById(R.id.ivPlanner);
        ivMusic = findViewById(R.id.ivMusic);
        ivAbout = findViewById(R.id.ivAbout);

        tvHomeLabel = findViewById(R.id.tvHomeLabel);
        tvPlannerLabel = findViewById(R.id.tvPlannerLabel);
        tvMusicLabel = findViewById(R.id.tvMusicLabel);
        tvAboutLabel = findViewById(R.id.tvAboutLabel);

        // Bind Content Images
        ivFeaturedImage = findViewById(R.id.ivFeaturedImage);
        ivForestItem = findViewById(R.id.ivForestItem);
        ivMountainsItem = findViewById(R.id.ivMountainsItem);
        ivDesertItem = findViewById(R.id.ivDesertItem);
        ivHillItem = findViewById(R.id.ivHillItem);

        loadDynamicImages();

        setupBottomNavigation();
        setActiveNavItem(navMusic);

        View imgForest = findViewById(R.id.imgPaintingForest);
        if (imgForest != null)
            imgForest.setOnClickListener(
                    v -> showNowPlaying("Forest Morning", "Nature Scene", R.drawable.green_forest, 1));

        View imgMtn = findViewById(R.id.imgMountaineers);
        if (imgMtn != null)
            imgMtn.setOnClickListener(
                    v -> showNowPlaying("Arctic Wind", "Deep Concentration", R.drawable.mountains, 2));

        View imgDesert = findViewById(R.id.imgLovelyDeserts);
        if (imgDesert != null)
            imgDesert
                    .setOnClickListener(v -> showNowPlaying("Night Crickets", "Calm Evening", R.drawable.desert, 3));

        View imgHill = findViewById(R.id.imgHillSides);
        if (imgHill != null)
            imgHill.setOnClickListener(v -> showNowPlaying("Mountain River", "Flow State", R.drawable.hill, 4));

    }

    private void loadDynamicImages() {
        // Attempt to load URLs from Firestore first
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("music_assets")
                .document("urls")
                .get()
                .addOnSuccessListener(doc -> {
                    String fUrl = doc.getString("featured");
                    String foUrl = doc.getString("forest");
                    String mUrl = doc.getString("mountains");
                    String dUrl = doc.getString("desert");
                    String hUrl = doc.getString("hill");

                    // Audio URLs from Firestore
                    String afUrl = doc.getString("audio_featured");
                    String afoUrl = doc.getString("audio_forest");
                    String amUrl = doc.getString("audio_mountains");
                    String adUrl = doc.getString("audio_desert");
                    String ahUrl = doc.getString("audio_hill");

                    if (afUrl != null)
                        dynamicStreamUrls[0] = afUrl;
                    if (afoUrl != null)
                        dynamicStreamUrls[1] = afoUrl;
                    if (amUrl != null)
                        dynamicStreamUrls[2] = amUrl;
                    if (adUrl != null)
                        dynamicStreamUrls[3] = adUrl;
                    if (ahUrl != null)
                        dynamicStreamUrls[4] = ahUrl;

                    applyGlideLoading(
                            fUrl != null ? fUrl : URL_FEATURED,
                            foUrl != null ? foUrl : URL_FOREST,
                            mUrl != null ? mUrl : URL_MOUNTAINS,
                            dUrl != null ? dUrl : URL_DESERT,
                            hUrl != null ? hUrl : URL_HILL);
                })
                .addOnFailureListener(e -> {
                    // Fallback to defaults on failure
                    applyGlideLoading(URL_FEATURED, URL_FOREST, URL_MOUNTAINS, URL_DESERT, URL_HILL);
                });
    }

    private void applyGlideLoading(String f, String fo, String m, String d, String h) {
        if (ivFeaturedImage != null) {
            Glide.with(this).load(f).error(R.drawable.forest).transition(DrawableTransitionOptions.withCrossFade())
                    .into(ivFeaturedImage);
        }
        if (ivForestItem != null) {
            Glide.with(this).load(fo).error(R.drawable.green_forest)
                    .transition(DrawableTransitionOptions.withCrossFade()).into(ivForestItem);
        }
        if (ivMountainsItem != null) {
            Glide.with(this).load(m).error(R.drawable.mountains).transition(DrawableTransitionOptions.withCrossFade())
                    .into(ivMountainsItem);
        }
        if (ivDesertItem != null) {
            Glide.with(this).load(d).error(R.drawable.forest).transition(DrawableTransitionOptions.withCrossFade())
                    .into(ivDesertItem);
        }
        if (ivHillItem != null) {
            Glide.with(this).load(h).error(R.drawable.green_forest)
                    .transition(DrawableTransitionOptions.withCrossFade()).into(ivHillItem);
        }
    }

    private void showNowPlaying(String title, String subtitle, int imageRes, int trackIndex) {
        boolean isSameTrack = (currentTrackIndex == trackIndex && mediaPlayer != null);

        if (!isSameTrack) {
            stopMusic();
            currentTrackIndex = trackIndex;
        }

        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(
                this, R.style.BottomSheetDialogTheme);
        android.view.View view = getLayoutInflater().inflate(R.layout.layout_now_playing, null);
        dialog.setContentView(view);

        TextView tvTitle = view.findViewById(R.id.tvNowPlayingTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvNowPlayingSubtitle);
        ImageView ivArt = view.findViewById(R.id.ivNowPlayingArt);
        android.widget.SeekBar seekBar = view.findViewById(R.id.seekBar);
        TextView tvCurrent = view.findViewById(R.id.tvCurrentTime);
        TextView tvTotal = view.findViewById(R.id.tvTotalTime);
        ImageView btnPlayPause = view.findViewById(R.id.btnPlayPause);

        if (tvTitle != null)
            tvTitle.setText(title);
        if (tvSubtitle != null)
            tvSubtitle.setText(subtitle);
        if (ivArt != null)
            ivArt.setImageResource(imageRes);

        // Beat animation on play button
        android.view.animation.ScaleAnimation beatAnim = new android.view.animation.ScaleAnimation(1.0f, 1.08f, 1.0f,
                1.08f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f);
        beatAnim.setDuration(600);
        beatAnim.setRepeatMode(android.view.animation.Animation.REVERSE);
        beatAnim.setRepeatCount(android.view.animation.Animation.INFINITE);

        if (!isSameTrack) {
            // Init MediaPlayer with streaming URL
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build());

            try {
                String source = dynamicStreamUrls[trackIndex];
                if (source.startsWith("http")) {
                    Toast.makeText(this, "Buffering sound...", Toast.LENGTH_SHORT).show();
                    Map<String, String> headers = new HashMap<>();
                    headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                    mediaPlayer.setDataSource(this, Uri.parse(source), headers);
                } else {
                    mediaPlayer.setDataSource(this, Uri.parse(source));
                }
                mediaPlayer.prepareAsync();

                mediaPlayer.setOnPreparedListener(mp -> {
                    mp.start();
                    isPlaying = true;
                    if (btnPlayPause != null)
                        btnPlayPause.startAnimation(beatAnim);

                    int duration = mp.getDuration();
                    if (seekBar != null && duration > 0) {
                        seekBar.setMax(duration);
                        if (tvTotal != null)
                            tvTotal.setText(formatTime(duration));
                    }
                    startSeekUpdater(seekBar, tvCurrent);
                });
            } catch (Exception e) {
                Toast.makeText(this, "Cannot play: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            // Re-sync UI with currently playing track
            if (isPlaying && btnPlayPause != null) {
                btnPlayPause.startAnimation(beatAnim);
            }
            int duration = mediaPlayer.getDuration();
            if (seekBar != null && duration > 0) {
                seekBar.setMax(duration);
                if (tvTotal != null)
                    tvTotal.setText(formatTime(duration));
            }
            startSeekUpdater(seekBar, tvCurrent);
        }

        if (!isSameTrack && mediaPlayer != null) {
            // SeekBar drag handling for new playbacks
            if (seekBar != null) {
                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                        if (fromUser && mediaPlayer != null) {
                            mediaPlayer.seekTo(progress);
                            if (tvCurrent != null)
                                tvCurrent.setText(formatTime(progress));
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar sb) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar sb) {
                    }
                });
            }
        } else if (isSameTrack && seekBar != null) {
            // Attach listener for re-opened sync
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                    if (fromUser && mediaPlayer != null) {
                        mediaPlayer.seekTo(progress);
                        if (tvCurrent != null)
                            tvCurrent.setText(formatTime(progress));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar sb) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar sb) {
                }
            });
        }

        // Toggle play/pause
        if (btnPlayPause != null) {
            btnPlayPause.setOnClickListener(v -> {
                if (mediaPlayer == null)
                    return;
                if (isPlaying) {
                    mediaPlayer.pause();
                    isPlaying = false;
                    btnPlayPause.clearAnimation();
                } else {
                    mediaPlayer.start();
                    isPlaying = true;
                    btnPlayPause.startAnimation(beatAnim);
                    seekHandler.post(seekRunnable);
                }
            });
        }

        ImageView btnSkipNext = view.findViewById(R.id.btnSkipNext);
        ImageView btnSkipPrev = view.findViewById(R.id.btnSkipPrev);

        if (btnSkipNext != null) {
            btnSkipNext.setOnClickListener(v -> {
                dialog.dismiss();
                int nextIndex = (currentTrackIndex + 1) % dynamicStreamUrls.length;
                showNowPlaying(TRACK_TITLES[nextIndex], TRACK_SUBTITLES[nextIndex], TRACK_IMAGES[nextIndex], nextIndex);
            });
        }

        if (btnSkipPrev != null) {
            btnSkipPrev.setOnClickListener(v -> {
                dialog.dismiss();
                int prevIndex = (currentTrackIndex - 1 + dynamicStreamUrls.length) % dynamicStreamUrls.length;
                showNowPlaying(TRACK_TITLES[prevIndex], TRACK_SUBTITLES[prevIndex], TRACK_IMAGES[prevIndex], prevIndex);
            });
        }

        if (!isSameTrack && mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                if (btnPlayPause != null)
                    btnPlayPause.clearAnimation();
                if (seekBar != null)
                    seekBar.setProgress(0);
                if (tvCurrent != null)
                    tvCurrent.setText("00:00");
                currentTrackIndex = -1;
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "⚠ Streaming failed. Check connection.", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        dialog.setOnDismissListener(d -> seekHandler.removeCallbacksAndMessages(null));
        dialog.show();
    }

    private void startSeekUpdater(SeekBar seekBar, TextView tvCurrent) {
        seekHandler.removeCallbacksAndMessages(null);
        seekRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying && mediaPlayer.isPlaying()) {
                    int pos = mediaPlayer.getCurrentPosition();
                    if (seekBar != null)
                        seekBar.setProgress(pos);
                    if (tvCurrent != null)
                        tvCurrent.setText(formatTime(pos));
                    seekHandler.postDelayed(this, 500);
                }
            }
        };
        seekHandler.post(seekRunnable);
    }

    private void stopMusic() {
        seekHandler.removeCallbacksAndMessages(null);
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying())
                    mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.release();
            } catch (Exception ignored) {
            }
            mediaPlayer = null;
        }
        isPlaying = false;
        currentTrackIndex = -1;
    }

    private String formatTime(int millis) {
        int totalSec = millis / 1000;
        int m = totalSec / 60;
        int s = totalSec % 60;
        return String.format("%02d:%02d", m, s);
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v -> {
            setActiveNavItem(navHome);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                android.content.Intent intent = new android.content.Intent(MusicActivity.this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            }, 120);
        });
        navPlanner.setOnClickListener(v -> {
            setActiveNavItem(navPlanner);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                android.content.Intent intent = new android.content.Intent(MusicActivity.this,
                        AddPlannerActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            }, 120);
        });
        navMusic.setOnClickListener(v -> setActiveNavItem(navMusic));
        navAbout.setOnClickListener(v -> {
            setActiveNavItem(navAbout);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                android.content.Intent intent = new android.content.Intent(MusicActivity.this, ProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            }, 120);
        });
    }

    @Override
    public void onBackPressed() {
        android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        seekHandler.removeCallbacksAndMessages(null);
    }

    private void setActiveNavItem(LinearLayout activeItem) {
        // Simple color change for active state
        resetNavItem(navHome, ivHome, tvHomeLabel);
        resetNavItem(navPlanner, ivPlanner, tvPlannerLabel);
        resetNavItem(navMusic, ivMusic, tvMusicLabel);
        resetNavItem(navAbout, ivAbout, tvAboutLabel);

        // Set active item to black
        if (activeItem == navHome) {
            ivHome.setColorFilter(Color.BLACK);
            tvHomeLabel.setTextColor(Color.BLACK);
        } else if (activeItem == navPlanner) {
            ivPlanner.setColorFilter(Color.BLACK);
            tvPlannerLabel.setTextColor(Color.BLACK);
        } else if (activeItem == navMusic) {
            ivMusic.setColorFilter(Color.BLACK);
            tvMusicLabel.setTextColor(Color.BLACK);
        } else if (activeItem == navAbout) {
            ivAbout.setColorFilter(Color.BLACK);
            tvAboutLabel.setTextColor(Color.BLACK);
        }
    }

    private void resetNavItem(LinearLayout item, ImageView iv, TextView tv) {
        iv.setColorFilter(Color.parseColor("#888888"));
        tv.setTextColor(Color.parseColor("#888888"));
    }
}
