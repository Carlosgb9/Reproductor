package com.example.reproductor;

import static java.lang.String.format;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST = 1;
    private LinearLayout llCurrentSongInfo;
    private TextView tvSong, tvArtist, tvCurrentTime, tvTotalTime;
    private ImageButton bPlay, bBack, bNext, bLoop;
    private ListView lvPlayList;
    private SeekBar sbTime;
    private MediaPlayer mp;
    private int currentSongPosition;
    private boolean loop = false;

    private List<Cancion> listaCancion = new ArrayList<Cancion>();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolBar);

        //llCurrentSongInfo = findViewById(R.id.llCurrentSongInfo);
        tvSong = findViewById(R.id.tvSong);
        tvArtist = findViewById(R.id.tvArtist);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        bPlay = findViewById(R.id.bPlay);
        bBack = findViewById(R.id.bBack);
        bNext = findViewById(R.id.bNext);
        bLoop = findViewById(R.id.bLoop);
        lvPlayList = findViewById(R.id.lvPlayList);
        sbTime = findViewById(R.id.sbTime);

        checkPermissions();

        bPlay.setOnClickListener(view -> bOnClickPlayPauseSong());

        bLoop.setOnClickListener(view -> {
            bOnClickChangeLoop();
        });

        bNext.setOnClickListener(view -> {
            bOnClickNextSong();
        });

        bBack.setOnClickListener(view -> {
            bOnClickPreviousSong();
        });

        lvPlayList.setOnItemClickListener((parent, view, position, id) -> {
            bPlaySelectedSong(position);
        });

        if (mp == null) {
            llCurrentSongInfo.setVisibility(View.GONE);
        } else {
            bPlaySelectedSong(lvPlayList.getSelectedItemPosition());
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // TODO make song data smaller
        }
    }

    private void bPlaySelectedSong(int position) {
        Cancion chosenSong = (Cancion) lvPlayList.getItemAtPosition(position);

        resetAndStartMediaPlayer(chosenSong);

        try {
            setSongDataIntoLayout(chosenSong);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        setSongDataIntoSeekbar(chosenSong);

        llCurrentSongInfo.setVisibility(View.VISIBLE);
    }

    private void checkLoop() { // checks if the "loop" button is active or not
        if (loop) {
            mp.release();
            mp.start();
        } else {
            bOnClickNextSong();
        }
    }

    private void bOnClickChangeLoop() {
        if (loop) {
            bLoop.setColorFilter(getColor(R.color.disabled), PorterDuff.Mode.MULTIPLY);
        } else {
            bLoop.setColorFilter(null);
        }
        loop = !loop;
    }

    private void bOnClickPlayPauseSong() {
        if (mp == null) {
            Toast.makeText(this, getString(R.string.toast_no_song), Toast.LENGTH_SHORT).show();
        } else {
            if (!mp.isPlaying()) {
                mp.start();

                bPlay.setImageResource(R.drawable.pause_24);
            } else {
                mp.pause();

                bPlay.setImageResource(R.drawable.play_24);
            }

            mp.setOnCompletionListener(arg01 -> checkLoop());
        }
    }

    private void bOnClickNextSong() {
        if (currentSongPosition < lvPlayList.getCount() - 1) {
            currentSongPosition++;
        } else {
            lvPlayList.setSelection(0);

            currentSongPosition = 0;
        }

        bPlaySelectedSong(currentSongPosition);
    }

    private void bOnClickPreviousSong() {
        if (mp.getCurrentPosition() < 3000) {
            if (currentSongPosition > 0) {
                currentSongPosition--;
            } else {
                currentSongPosition = (lvPlayList.getCount() - 1);
            }

            bPlaySelectedSong(currentSongPosition);
        } else {
            resetAndStartMediaPlayer((Cancion) lvPlayList.getItemAtPosition(currentSongPosition));
        }
    }

    private void resetAndStartMediaPlayer(Cancion cancion) {
        if (mp != null) {
            mp.release();
            mp = null;
        }
        mp = MediaPlayer.create(MainActivity.this, cancion.getSongUri());
        mp.setOnCompletionListener(mediaPlayer -> checkLoop());
        mp.start();

        bPlay.setImageResource(R.drawable.pause_24);
    }

    private void setSongDataIntoLayout(Cancion cancion) throws IOException {
        tvSong.setText(cancion.getName());
        tvArtist.setText(cancion.getArtist());
    }

    private void setSongDataIntoSeekbar(Cancion cancion) { // loads the song duration into the seekbar
        sbTime.setMax(cancion.getDuration() / 1000);

        tvTotalTime.setText(durationConverter(cancion.getDuration()));
        tvCurrentTime.setText(durationConverter(0));
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_MEDIA_AUDIO)) {
                // permission was granted, yay! Do the external storage task you need to do.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_AUDIO}, MY_PERMISSIONS_REQUEST);
                // MY_PERMISSIONS_REQUEST is an app-defined int constant. The callback method gets the result of the request.
            }
        } else {
            updateSongList();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean mediaAudioPermission = false;
        boolean externalStoragePermission = false;

        if (requestCode == MY_PERMISSIONS_REQUEST) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) { // permission was granted, yay! Do the external storage task you need to do.
                updateSongList();
            }
        }
    }

    public void updateSongList() {
        String[] projection = new String[] {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM_ID,
        };
        String selection = MediaStore.Audio.Media.DURATION + " >= ? AND " + MediaStore.Audio.AudioColumns.DATA + " NOT LIKE ?";

        String[] selectionArgs = new String[] {
                String.valueOf(TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES)), "%/WhatsApp/%"
        };
        String sortOrder = MediaStore.Audio.Media.DISPLAY_NAME + " ASC";

        Cursor songDataCursor = getApplicationContext().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
        );

        int idColumn = songDataCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
        int nameColumn = songDataCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
        int durationColumn = songDataCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
        int artistColumn = songDataCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
        int albumIdColumn = songDataCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

        while (songDataCursor.moveToNext()) {
            long songId = songDataCursor.getLong(idColumn);
            String name = songDataCursor.getString(nameColumn);
            int duration = songDataCursor.getInt(durationColumn);
            String artist = songDataCursor.getString(artistColumn);
            long albumId = songDataCursor.getLong(albumIdColumn);

            Uri songContentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId);

            Bitmap albumThumbnail = null;
            try {
                albumThumbnail = getApplicationContext().getContentResolver().loadThumbnail(ContentUris.withAppendedId(
                        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId), new Size(100, 100), null);
            } catch (IOException e) {

            }

            listaCancion.add(new Cancion(songContentUri, name, duration, artist, albumThumbnail));

            ArrayAdapter<Cancion> arrayAdapter = new ArrayAdapter<Cancion>( this, android.R.layout.simple_list_item_1, listaCancion);

            lvPlayList.setAdapter(arrayAdapter);
        }
        songDataCursor.close();
    }



    private String durationConverter(int duration) {
        int minutes = duration / 1000 / 60;
        int remainingSeconds = (duration / 1000) % 60;

        return format(Locale.getDefault(),"%02d:%02d", minutes, remainingSeconds);
    }

}