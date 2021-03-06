package flat.uxml;

import flat.resources.Dimension;
import flat.resources.DimensionStream;
import flat.resources.StringBundle;
import flat.widget.*;
import flat.widget.image.ImageView;
import flat.widget.layout.*;
import flat.widget.selection.*;
import flat.widget.text.*;

import javafx.util.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class UXLoader {
    private static HashMap<String, UXGadgetFactory> builders = new HashMap<>();
    public static void install(String name, UXGadgetFactory gadgetFactory) {
        builders.put(name, gadgetFactory);
    }

    static {
        UXLoader.install("Scene", Scene::new);
        UXLoader.install("Box", Box::new);
        UXLoader.install("VBox", VBox::new);
        UXLoader.install("HBox", HBox::new);
        UXLoader.install("Button", Button::new);
        UXLoader.install("ToggleButton", ToggleButton::new);
        UXLoader.install("ToggleGroup", RadioGroup::new);
        UXLoader.install("Label", Label::new);
        UXLoader.install("ImageView", ImageView::new);
        UXLoader.install("Checkbox", Checkbox::new);
        UXLoader.install("RadioButton", RadioButton::new);
        UXLoader.install("RadioGroup", RadioGroup::new);
        UXLoader.install("Switch", Switch::new);
    }

    private DimensionStream dimensionStream;
    private Dimension dimension;
    private StringBundle stringBundle;
    private UXTheme theme;

    private Controller controller;

    private ArrayList<Pair<String, UXGadgetLinker>> linkers = new ArrayList<>();
    private HashMap<String, Gadget> targets = new HashMap<>();

    // TODO - DEBUG
    private ArrayList<String> logger = new ArrayList<>();

    public UXLoader(DimensionStream dimensionStream, Dimension dimension) {
        this(dimensionStream, dimension, null);
    }

    public UXLoader(DimensionStream dimensionStream, Dimension dimension, UXTheme theme) {
        this(dimensionStream, dimension, theme, null, null);
    }

    public UXLoader(DimensionStream dimensionStream, Dimension dimension, UXTheme theme, StringBundle stringBundle, Controller controller) {
        setDimensionStream(dimensionStream);
        setDimension(dimension);
        setStringBundle(stringBundle);
        setController(controller);
        setTheme(theme);
    }

    public DimensionStream getDimensionStream() {
        return dimensionStream;
    }

    public UXLoader setDimensionStream(DimensionStream dimensionStream) {
        this.dimensionStream = dimensionStream;
        return this;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public UXLoader setDimension(Dimension dimension) {
        this.dimension = dimension;
        return this;
    }

    public UXTheme getTheme() {
        return theme;
    }

    public UXLoader setTheme(UXTheme theme) {
        this.theme = theme;
        return this;
    }

    public StringBundle getStringBundle() {
        return stringBundle;
    }

    public UXLoader setStringBundle(StringBundle stringBundle) {
        this.stringBundle = stringBundle;
        return this;
    }

    public Object getController() {
        return controller;
    }

    public UXLoader setController(Controller controller) {
        this.controller = controller;
        return this;
    }

    void addLink(String id, UXGadgetLinker linker) {
        linkers.add(new Pair<>(id, linker));
    }

    void link() {
        for (Pair<String, UXGadgetLinker> linker : linkers) {
            linker.getValue().onLink(targets.get(linker.getKey()));
        }
        linkers.clear();
        targets.clear();
    }

    public void log(String log) {
        logger.add(log);
    }

    public List<String> getLog() {
        return logger;
    }

    public Widget load() throws Exception {
        return load(false);
    }

    public Widget load(boolean createScene) throws Exception {
        InputStream inputStream = dimensionStream.getStream(dimension);
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputStream);
            doc.getDocumentElement().normalize();

            Widget uxml = null;
            NodeList nList = doc.getChildNodes();
            for (int i = 0; i < nList.getLength(); i++) {
                Node node = nList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    if (uxml != null) {
                        log("Unexpected root node : " + node.getNodeName());
                    } else {
                        Gadget gadget = recursive(controller, node);
                        if (gadget == null) {
                            log("Unexpected root node : " + node.getNodeName());
                        } else {
                            uxml = gadget.getWidget();
                            if (uxml == null) {
                                log("Unexpected root node : " + node.getNodeName());
                            }
                        }
                    }
                }
            }

            link();

            if (createScene && !(uxml instanceof Scene)) {
                Scene scene = new Scene();
                scene.add(uxml);
                return scene;
            } else {
                return uxml;
            }
        } finally {
            try {
                inputStream.close();
            } catch (IOException ignored) {
            }
        }
    }

    private Gadget recursive(Controller controller, Node node) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            //Name
            String name = node.getNodeName();

            //Factory
            UXGadgetFactory gadgetFactory = builders.get(name);

            if (gadgetFactory != null) {
                Gadget gadget = gadgetFactory.build();

                //Attributes
                UXStyle style = null;
                HashMap<String, UXValue> values = new HashMap<>();

                NamedNodeMap nnm = node.getAttributes();
                for (int i = 0; i < nnm.getLength(); i++) {
                    Node item = nnm.item(i);
                    String att = item.getNodeName();
                    String value = item.getNodeValue();
                    if (value == null) {
                        value = "true";
                    } else if (value.startsWith("$") && stringBundle != null) {
                        value = stringBundle.get(value.substring(1));
                    } else if (value.startsWith("\\$")) {
                        value = value.substring(1);
                    }
                    if (att.equals("style")) {
                        style = theme.getStyle(value);
                    } else {
                        values.put(att, new UXValue(value));
                    }
                }
                gadget.applyAttributes(new UXStyleAttrs("attributes", style, this, values), controller);
                if (gadget.getId() != null) {
                    targets.put(gadget.getId(), gadget);
                }

                //Children
                NodeList nList = node.getChildNodes();
                UXChildren children = new UXChildren(this);
                for (int i = 0; i < nList.getLength(); i++) {
                    Gadget child = recursive(controller, nList.item(i));
                    if (child != null) {
                        children.add(child);
                    }
                }
                gadget.applyChildren(children);

                children.logUnusedChildren();
                return gadget;
            } else {
                log("Widget not found : " + name);
            }
        }
        return null;
    }

    public boolean hasLog() {
        return logger.size() > 0;
    }

    public void printLog() {
        System.err.println("Imperfect UXML decode : " + dimensionStream.getName());
        for (String log : logger) System.err.println("    " + log);
    }
}