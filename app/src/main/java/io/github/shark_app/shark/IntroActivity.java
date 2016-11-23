package io.github.shark_app.shark;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

import butterknife.BindColor;
import butterknife.BindString;
import butterknife.ButterKnife;

/**
 * Created by Divay Prakash on 23-Nov-16.
 */

public class IntroActivity extends AppIntro {
    @BindColor(R.color.fbutton_color_alizarin) int slide1background;
    @BindColor(R.color.fbutton_color_nephritis) int slide2background;
    @BindColor(R.color.fbutton_color_carrot) int slide3background;
    @BindColor(R.color.colorPrimary) int slide4background;
    @BindString(R.string.slide1title) String slide1title;
    @BindString(R.string.slide2title) String slide2title;
    @BindString(R.string.slide3title) String slide3title;
    @BindString(R.string.slide4title) String slide4title;
    @BindString(R.string.slide1description) String slide1description;
    @BindString(R.string.slide2description) String slide2description;
    @BindString(R.string.slide3description) String slide3description;
    @BindString(R.string.slide4description) String slide4description;
    int slide1image = R.drawable.slide1image;
    int slide2image = R.drawable.slide23image;
    int slide3image = R.drawable.slide23image;
    int slide4image = R.drawable.slide4image;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);
        addSlide(AppIntroFragment.newInstance(slide1title, slide1description, slide1image, slide1background));
        addSlide(AppIntroFragment.newInstance(slide2title, slide2description, slide2image, slide2background));
        addSlide(AppIntroFragment.newInstance(slide3title, slide3description, slide3image, slide3background));
        addSlide(AppIntroFragment.newInstance(slide4title, slide4description, slide4image, slide4background));
        showSkipButton(false);
        setDepthAnimation();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        this.finish();
    }
}