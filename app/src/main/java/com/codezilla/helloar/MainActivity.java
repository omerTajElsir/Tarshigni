package com.omertaj.tarshigni;

import android.app.AlertDialog;
import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Display;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.Random;


import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private CustumArFragment arFragment;
    private Scene scene;
    private Camera camera;
    private ModelRenderable bulletRend;
    private boolean shootStarttartTimer = true;
    private int baloonsLeft=20;
    private Point point;
    private TextView ballonLeftTxt;
    private SoundPool soundPool;
    private int sound;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Display display=getWindowManager().getDefaultDisplay();
        point=new Point();
        display.getRealSize(point);
        setContentView(R.layout.activity_main);
        
        loadSoundPool();

        ballonLeftTxt=findViewById(R.id.balloonsCntTxt);

        arFragment= (CustumArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);
        scene=arFragment.getArSceneView().getScene();
        camera=scene.getCamera();
        addBallonsToScene();
        buildBulletModel();
        ImageView scope=(ImageView) findViewById(R.id.imgScope);

        ImageButton shoot = findViewById(R.id.shootButton);
        shoot.setOnClickListener(v -> {
            scope.startAnimation(AnimationUtils.loadAnimation(this,R.anim.shakeanimate));
            if (shootStarttartTimer){
                startTimer();
                shootStarttartTimer=false;
            }
            shoot();
        });

    }

    private void loadSoundPool() {
        AudioAttributes audioAttributes=new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_GAME)
                .build();
        soundPool=new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build();

        sound=soundPool.load(this,R.raw.ballonsound,1);
    }

    private void shoot() {
        Ray ray= camera.screenPointToRay(point.x/2f,point.y/2f);
        Node node =new Node();
        node.setRenderable(bulletRend);
        scene.addChild(node);

        new Thread(()->{
            for (int i=0;i<200;i++){
                int finalI=i;
                runOnUiThread(()->{
                    Vector3 vector3 = ray.getPoint(finalI*0.1f);
                    node.setWorldPosition(vector3);

                    Node nodeInContact = scene.overlapTest(node);
                    if(nodeInContact!=null){
                        baloonsLeft--;
                        ballonLeftTxt.setText("Balloons left : "+ baloonsLeft);
                        scene.removeChild(nodeInContact);
                        soundPool.play(sound,1f,1f,1,0,1f);
                    }
                });

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            runOnUiThread(()-> scene.removeChild(node));
        }).start();
    }

    private void startTimer() {
        TextView timer = findViewById(R.id.timerText);

        new Thread(()->{
            int seconds=0;
            while (baloonsLeft>0){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            seconds++;
            int minitsPassed = seconds/60;
            int secondsPassed = seconds%60;

            runOnUiThread(()->{
                timer.setText(minitsPassed+" : "+secondsPassed);
            });
        }).start();

    }

    private void buildBulletModel() {
        Texture.builder().setSource(this,R.drawable.texture).build().thenAccept(texture -> {
            MaterialFactory
                    .makeOpaqueWithTexture(this,texture)
                    .thenAccept(material -> {
                        bulletRend = ShapeFactory
                                .makeSphere(0.01f,new Vector3(0f,0f,0f),material);
                    });
        });
    }

    private void addBallonsToScene() {
        ModelRenderable.builder()
                .setSource(this, Uri.parse("Balloon.sfb"))
                .build()
                .thenAccept(renderable ->{
                    for (int i=0;i<20;i++){
                        Node node=new Node();
                        node.setRenderable(renderable);
                        scene.addChild(node);

                        Random random=new Random();
                        int x=random.nextInt(10);
                        int y=random.nextInt(10);
                        int z=random.nextInt(20);

                        z=-z;

                        node.setWorldPosition(new Vector3(
                                (float) x,
                                y/10f,
                                (float) z
                        ));


                    }
                })
                .exceptionally(throwable -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(throwable.getMessage()).show();
                    return null;
                });
    }

    private void addModelToScene(Anchor anchor, ModelRenderable modelRenderable) {
        AnchorNode anchorNode=new AnchorNode(anchor);
        TransformableNode transformableNode=new TransformableNode(arFragment.getTransformationSystem());
        transformableNode.setParent(anchorNode);
        transformableNode.setRenderable(modelRenderable);
        arFragment.getArSceneView().getScene().addChild(anchorNode);
        transformableNode.select();
    }
}
