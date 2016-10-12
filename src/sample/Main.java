package sample;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javassist.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;


public class Main extends Application {
    public String pathToJar ="";
    public static final ObservableList data = FXCollections.observableArrayList();
    Class c = null;
    Object[] args = null;
    static ClassPool pool = ClassPool.getDefault();
    static Loader cl = new Loader(pool);

    @Override
    public void init(){}
    @Override
    public void start(Stage primaryStage) throws IOException, CannotCompileException, NotFoundException, ClassNotFoundException {
        TreeView<JarEntry> tree = new TreeView<>();
        tree.setShowRoot(false);
        TreeItem<JarEntry> root = new TreeItem<>();
        tree.setRoot(root);

        ListView<Method> methodListView = new ListView<>();
        methodListView.setItems(data);
        // only display last portion of the path in the cells:
        tree.setCellFactory(tv -> new TreeCell<JarEntry>() {
            @Override
            public void updateItem(JarEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    String[] pathElements = item.getName().split("/");
                    setText(pathElements[pathElements.length - 1]);
                }
            }
        });

        Text selectedClassName = new Text("Loaded class name");
        Text status = new Text("Status");
        tree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {

            Method[] methods= null;
            data.clear();
            if(newValue.getValue().getName().length() > 6) {
//                    System.out.println(newValue.getValue().getName().substring(newValue.getValue().getName().length() - 6));
                if ((newValue.getValue().getName().substring(newValue.getValue().getName().length() - 6)).equals(new String(".class"))) {


                    String className = newValue.getValue().getName().substring(0, newValue.getValue().getName().length() - new String(".class").length());
                    className = className.replace('/', '.');
                    selectedClassName.setText(className);
                    try {
                        URL[] urls = new URL[]{new URL("jar:file:" + pathToJar + "!/")};
                        URLClassLoader cl1 = URLClassLoader.newInstance(urls);
                        c = cl1.loadClass(className);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
//                methods  = c.getDeclaredMethods();
                    data.addAll(c.getDeclaredMethods());
                }
                else{
                    selectedClassName.setText("It's not a class");
            }

            }
        });

        methodListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Method>() {
            @Override
            public void changed(ObservableValue<? extends Method> observable, Method oldValue, Method newValue) {
                Type[] parameters = newValue.getGenericParameterTypes();
                if(typesAreSimple(parameters)) {

                    Object[] objects = createParams(parameters);
                    Object o3 = null;
//                System.out.print("obiekty do metody" + objects.toString());
                    try {
                        Constructor<?>[] constructors = c.getConstructors();
                        boolean simpleConstructorTypes = true;

                        for (Constructor<?> constructor : constructors) {
//                            System.out.println(constructor.toString());
                            Type[] constrTypes = constructor.getGenericParameterTypes();
                            simpleConstructorTypes = true;
                            for (Type type : constrTypes) {
                                if (!typeIsSimple(type)) { //jesli typ parametru konstruktora nie jest prosty
                                    simpleConstructorTypes = false;
                                    break;
                                }

                            }
                            if (simpleConstructorTypes) {
                                o3 = constructor.newInstance(createParams(constrTypes));
                                System.out.println("Object created");
                                break;

                            }

                        }
                        if(!simpleConstructorTypes)
                        {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Alert");
                            alert.setHeaderText(null);
                            alert.setContentText("Brak konstruktora przyjmujacego wylacznie typy proste");
                            alert.showAndWait();
                            return;
                        }
                        if (parameters.length > 0) {
                            Object result = newValue.invoke(o3, createParams(parameters));
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Wynik");
                            alert.setHeaderText(null);
                            alert.setContentText(result.toString());
                            alert.showAndWait();
                        } else {
                            Object result = newValue.invoke(o3);
                        }

//                    Object o = c.newInstance();
//                        System.out.println("\t: "  +o.toString() + ", ");
//                  } catch (ClassNotFoundException e) {
//                    e.printStackTrace();
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Alert");
                    alert.setHeaderText(null);
                    alert.setContentText("argumenty metody nie sa typami prostymi");
                    alert.showAndWait();

                }
            }
        });


        ObjectProperty<JarFile> jarFile = new SimpleObjectProperty<>();
        jarFile.addListener((obs, oldFile, newFile) -> {
            if (newFile == null) {
                root.getChildren().clear();
            } else {
                populateTree(root, newFile);
            }
        });

        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new ExtensionFilter("Jar Files", "*.jar"));
       // chooser.setInitialDirectory(new File("D:\\STUDIA\\Semestr 6\\JFK\\L2\\JFK_2\\JKF_L3_FX"));

        Button loadButton = new Button("Load jar file");

        loadButton.setOnAction(e -> {
            File file = chooser.showOpenDialog(primaryStage);
            if (file != null) {
                try {
                    jarFile.set(new JarFile(file));
                    setPathToJar(file.getAbsolutePath());
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });

        BorderPane uiRoot = new BorderPane(tree, selectedClassName, methodListView, loadButton, null );
        BorderPane.setMargin(selectedClassName, new Insets(10));
        BorderPane.setMargin(loadButton, new Insets(10));
        methodListView.setMinWidth(450);
        BorderPane.setAlignment(loadButton, Pos.CENTER);
        Scene scene = new Scene(uiRoot, 900, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("JAR VIEWER");
        primaryStage.show();
    }
    public boolean typeIsSimple(Type type){
        String typeName = type.getTypeName();
        switch (typeName){
            case "int":
                return true;
            case "double":
                return true;
            case "float":
                return true;
            case "bool":
                return true;
            case "char":
                return true;
            default:
                return false;
        }

    }
    public boolean typesAreSimple(Type[] types)
    {
        for(Type type: types)
        {
            if(typeIsSimple(type)){}
            else{
                return false;
            }
        }
        return true;
    }
    public Object[] createParams (Type[] parameters){
        Random generator = new Random();
        Object[] objects = new Object[parameters.length];
        for(int i=0; i<parameters.length;i++){
            //Type par: parameters)
            String type = parameters[i].getTypeName();
//            System.out.print("type: " + type);
            switch (type){
                case "int":
                    int x = generator.nextInt(10);
                    objects[i] = x;
                    break;
                case "double":
                    double z = generator.nextDouble() *10;
                    objects[i] = z;
                    break;
                case "float":
                    float q = generator.nextFloat();
                    objects[i] = q;
                    break;
                case "bool":
                    boolean w = generator.nextBoolean();
                    objects[i] = w;
                    break;
                case "char":
                    char e = 'a';
                    objects[i] = e;
                    break;
            }
//            System.out.print(" object val: " + objects[i].toString() + "\n");

        }
//        System.out.println(objects.length);
        return objects;
    }

    public void selectedClass(Class cls)
    {
        Method[] methods = cls.getMethods();

        for(Method method: methods) {
            System.out.println(method.getName());
        }
    }

    public void packageClasses(){ //klasy z pakietu do zmiany dziedziczenia
        ClassPath p = null;
        try {
            p = ClassPath.from(ClassLoader.getSystemClassLoader());
        } catch (IOException e) {
            e.printStackTrace();
        }
        ImmutableSet<ClassPath.ClassInfo> classes = p.getTopLevelClasses("sample");
        System.out.println("classes: " + classes);
    }

    private void populateTree(TreeItem<JarEntry> root, JarFile file) {
        root.getChildren().clear();
        List<JarEntry> entries = file.stream().collect(Collectors.toList());
        entries.sort(Comparator
                .comparing((JarEntry entry) -> entry.getName().split("/").length)
                .thenComparing(entry -> {
                    String[] pathElements = entry.getName().split("/");
                    return pathElements[pathElements.length - 1];
                }));

        for (JarEntry entry : entries) {
            List<String> pathElements = Arrays.asList(entry.getName().split("/"));
            TreeItem<JarEntry> parent = root;
            for (int i = 0; i < pathElements.size() - 1 ; i++) {
                String matchingName = String.join("/", pathElements.subList(0, i+1));
                final TreeItem<JarEntry> current = parent ;
                parent = current.getChildren().stream()
                        .filter(child -> child.getValue().getName().equals(matchingName))
                        .findFirst()
                        .orElseGet(() -> {
                            JarEntry newEntry = new JarEntry(matchingName);
                            TreeItem<JarEntry> newItem = new TreeItem<>(newEntry);
                            current.getChildren().add(newItem);
                            return newItem ;
                        });
            }
            parent.getChildren().add(new TreeItem<>(entry));
        }
    }
    private static void modifyClassHierachy(Class<?> mainClass, Class<?> child)
            throws NotFoundException, CannotCompileException, IOException {
        CtClass ct = pool.get(child.getCanonicalName());
        ct.setSuperclass(pool.get(mainClass.getCanonicalName()));
        ct.writeFile();

    }
    private static void printClassHierarchy(Class<?> cls, boolean initial) {
        if (initial) {
            System.out.println("Hierarchia klasy " + cls);
            printClassHierarchy(cls.getSuperclass(), false);
        } else if (cls != null) {
            System.out.println(cls);
            printClassHierarchy(cls.getSuperclass(), false);
        } else {
            System.out.println("################################");
        }

    }
    public void setPathToJar(String pathToJar1){
        this.pathToJar = pathToJar1;
    }
    public String getPathToJar(){
        return pathToJar;
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException,CannotCompileException, NotFoundException {
        launch(args);
        modifyClassHierachy(A.class, B.class);
        printClassHierarchy(cl.loadClass(A.class.getCanonicalName()), true);
        printClassHierarchy(cl.loadClass(B.class.getCanonicalName()), true);
    }
}