/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package kodemat.visu;

import kodemat.visu.appstates.VisuHazelcastAppState;
import kodemat.visu.appstates.NiftyGuiAppState;
import kodemat.touch.visu.appstates.TouchCameraControllerAppState;
import kodemat.visu.appstates.VisuClientKeyMappingAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.control.BillboardControl;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.ui.Picture;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import kodemat.visu.appstates.NiftyConnectAppState;
import kodemat.visu.appstates.VisuScenegraphAppState;
import com.jme3.system.AppSettings;
import de.lessvoid.nifty.Nifty;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import kodemat.admin.VisuKodematUser;
import kodemat.touch.visu.editor.TouchVisuEditor;
import kodemat.touch.visu.input.multitouch.MultiTouchAdapter;
import org.openide.util.Exceptions;


/**
 *
 * @author moritz
 */
public class TouchVisuClient extends SimpleApplication implements IKoDeMatClient {

    private static String serverAdress;
    private static VisuKodematUser kodematUser;
    Node modelNode = new Node("Scene");
//   private VisuHazelcastAppState hazelcastAppState = new VisuHazelcastAppState(new VisuClientConnectionInfo("10.0.2.251:5801",null,null,null));
    private VisuHazelcastAppState hazelcastAppState;
//    private VisuClientStateViewAppState clientStateViewAppState = new VisuClientStateViewAppState(guiNode, guiFont);
    private VisuClientKeyMappingAppState clientKeyMappingAppState = new VisuClientKeyMappingAppState();
//    private VisuScenegraphAppState clientScenegraphAppState = new VisuScenegraphAppState(hazelcastAppState, modelNode);
    private TouchCameraControllerAppState cameraControllerAppState = new TouchCameraControllerAppState(modelNode);
    private NiftyGuiAppState niftyGuiAppState;
    private NiftyConnectAppState niftyConnectAppState;
    private ScreenshotAppState screenShotState = new ScreenshotAppState();
    private static Properties globalProperties;

    
    
    private boolean connected;

    
       public TouchVisuClient() { 
        super(new StatsAppState());
        this.setUpUserProperties();
        this.setSettings(this.getAndSetSettings());
    }
    
    @Override
    public void simpleInitApp() {
        NiftyJmeDisplay niftyDisplay = new NiftyJmeDisplay(this.getAssetManager(),
                this.getInputManager(),
                this.getAudioRenderer(),
                this.getGuiViewPort());
        Nifty nifty = niftyDisplay.getNifty();

        this.getGuiViewPort().addProcessor(niftyDisplay);
        niftyGuiAppState = new NiftyGuiAppState(nifty, false);
        niftyConnectAppState = new NiftyConnectAppState(nifty, clientKeyMappingAppState, globalProperties, this);
    
        setPauseOnLostFocus(false);

        initRooteNode();
//        setUpBackground();
        //these are to display or hide the scene info such as vertex and texture numbers
        stateManager.getState(StatsAppState.class).setDisplayStatView(false);
        stateManager.getState(StatsAppState.class).setDisplayFps(false);

//        attach the appstates
        stateManager.attach(niftyGuiAppState);
        stateManager.attach(niftyConnectAppState);     
        stateManager.attach(clientKeyMappingAppState);
        stateManager.attach(cameraControllerAppState);      
        stateManager.attach(screenShotState);



        cam.setLocation(new Vector3f(0, 5, 10));

        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-1, -1, 2).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        DirectionalLight sun1 = new DirectionalLight();
        sun1.setDirection(new Vector3f(-0.57735026f, 0.57735026f, 0.57735026f));
        sun1.setColor(ColorRGBA.White);

        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.DarkGray);
        PointLight pointLight = new PointLight();
        pointLight.setColor(ColorRGBA.White);
        pointLight.setPosition(new Vector3f(0, 4, 0));
        pointLight.setRadius(20);
//        rootNode.addLight(pointLight);

        
                rootNode.addLight(sun);
//        rootNode.addLight(sun1);
        rootNode.addLight(ambient);
        
        settings.setFrameRate(40);
        viewPort.setBackgroundColor(ColorRGBA.LightGray);
           
//       addCoordinatesToScene();
        
        /* Drop shadows */
        final int SHADOWMAP_SIZE=1024;
        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, SHADOWMAP_SIZE, 3);
        dlsr.setLight(sun);
        viewPort.addProcessor(dlsr);

        
        //if there the initial connection field nifty window is not attached then connect 
        //using default values (from prop.properties file
        if(this.getStateManager().getState(NiftyConnectAppState.class)==null)
        {
            this.createConnectionInfo(null, kodematUser);
            this.clientConnect();
            
        }

    }

    @Override
    public void createConnectionInfo(VisuClientConnectionInfo vcci, VisuKodematUser user) {
        if (vcci == null) {
            vcci = new VisuClientConnectionInfo(serverAdress, null, null, null);
        }
        if (user == null) {
            user = kodematUser;
        }
        
//        create the appstate that allows connection to the hazelcast instance and 
//                then add it to the stateManager
        hazelcastAppState = new VisuHazelcastAppState(vcci, user);
        stateManager.attach(hazelcastAppState);
         
        VisuScenegraphAppState clientScenegraphAppState = new VisuScenegraphAppState(hazelcastAppState, modelNode);
        stateManager.attach(clientScenegraphAppState);
         
    }
    
    public void setUpUserProperties(){
        kodematUser = new VisuKodematUser();
    
        Properties globalProps = new Properties();
        try {
            globalProps.load(new FileReader("./prop.properties"));
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }  
        globalProperties = globalProps;
        if(globalProperties != null){
        serverAdress = globalProps.getProperty("hazelcast.client.addresses");
        if (serverAdress == null || serverAdress.equals("")) {
            throw new IllegalStateException("Could not find server adress");
        } else {
            System.out.println("CONNECTING TO SERVER " + serverAdress);
        //TODO: the username should change, for now make it the same with name
        kodematUser.setUsername(globalProps.getProperty("kodemat.user.lastName"));
        kodematUser.setFirst_name(globalProps.getProperty("kodemat.user.firstName"));
        kodematUser.setLast_name(globalProps.getProperty("kodemat.user.lastName"));
        kodematUser.setPosition(globalProps.getProperty("kodemat.user.position"));
        kodematUser.setCompany(globalProps.getProperty("kodemat.user.company"));
        kodematUser.setSocketAddress(serverAdress);
        }
        }

      
        
    }
    
 /**
     * If not a runnable the thread get stuck while starting the hazelcast client
     */
    @Override
         public void clientConnect() {

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    
                        hazelcastAppState.client_start(); 
                        
                        //TODO: the next statement initialises the UI Controller (VisuEditor). It should always be called after the
//         hazelcast initialisation because the hazelcast client instance for the visuHelper should exist for the controller
                     TouchVisuEditor editor = new TouchVisuEditor(hazelcastAppState.getClient());
                       cameraControllerAppState.getEditorManager().push(editor);
                        sleep(100);
                      editor.initUI(); 
                     
                     try {
                        //!!!!
                        MultiTouchAdapter mt = new MultiTouchAdapter(editor, cameraControllerAppState);
                    } catch (Exception ex) {
                        Exceptions.printStackTrace(ex);
                    }
                       
                      
                      //caution!!! The cursor should be created always after the nifty init method, otherwise we get concurrent modification problems
                      cameraControllerAppState.createCursor();
                   
                  
                }
            };
            hazelcastAppState.threadPool.submit(r);
          
        
      
          
  
    }
    
       public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
        }
    }
 boolean connectedDone() {
    return connected;
}
    
    public  AppSettings getAndSetSettings(){
        
        AppSettings settings = new AppSettings(true);
        settings.setSettingsDialogImage("/Interface/Images/splash-small.jpg");
        settings.setTitle("KoDeMat Lightweight Platform");
        settings.setFrameRate(60);
        settings.setResolution(1280, 720);

   
        return settings;
    }

    public static void main(String[] args) {
        String return_value;
        return_value = System.setProperty("java.net.preferIPv4Stack", "true");
        System.out.println("Prefer IPV4 stack was previously :" + return_value);
        return_value = System.getProperty("java.net.preferIPv4Stack");
        System.out.println("prefer IPV4 stack has been set to :" + return_value);

        Logger.getLogger("").setFilter(new MyFilter());
        Logger.getLogger("com.jme3").setLevel(Level.WARNING);
        Logger.getLogger("com.hazelcast").setLevel(Level.WARNING);
        Logger.getLogger("de.lessvoid.nifty").setLevel(Level.WARNING);
        Logger.getLogger("NiftyInputEventHandlingLog").setLevel(Level.SEVERE);

//!!!!
        TouchVisuClient visuClient = new TouchVisuClient();
         visuClient.setSettings(visuClient.getAndSetSettings());
         
         visuClient.start();
    }


    public void addCoordinatesToScene() {

        for (int i = -20; i < 50; i++) {

            for (int j = -20; j < 50; j++) {
                BitmapFont myFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
                BitmapText text = new BitmapText(myFont);
                text.setSize(0.5f);
                text.setText("" + i + "," + j);
                text.setColor(ColorRGBA.White);
                BillboardControl bbControl = new BillboardControl();
                text.addControl(bbControl);
                text.setLocalTranslation(new Vector3f(i, 1, j));

                modelNode.attachChild(text);

                j = j + 1;
            }
            i = i + 1;
        }
    }

    // set up the Background Quad, which will be rendererd in its own renderpass
    private void setUpBackground() {


        Picture p = new Picture("background");
        p.setImage(assetManager, "Interface/Images/Kodemat_Background_small.png", false);
//        p.setImage(assetManager, "Interface/Images/Blue-Gradient-800x6001.jpg", false);

        p.setWidth(settings.getWidth());
        p.setHeight(settings.getHeight());
        p.setPosition(0, 0);


        ViewPort pv = renderManager.createPreView("background", cam);
        pv.setClearFlags(true, true, true);

        pv.attachScene(p);
// pv.setBackgroundColor(ColorRGBA.Red);

        viewPort.setClearFlags(false, true, true);
        p.updateGeometricState();
    }

    private void initRooteNode() {
        rootNode.attachChild(modelNode);
    }
}
class MyFilter implements Filter {

    public boolean isLoggable(LogRecord record) {
        System.out.println("LOGGER FILTER");
        return !record.getSourceMethodName().startsWith("process");
    }
}
