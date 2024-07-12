package com.example.demo;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.jgrapht.Graph;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.util.mxCellRenderer;

@Component
public class MyGraph implements CommandLineRunner {

    private static final int NUM_VERTEXES_TO_CREATE = 10000;
    private static final int GRAPHS_TO_CREATE = 1;

    @Override
    public void run(String... args) throws Exception {

        // Create and export 100 graphs as images
        for(int graphNumber=0; graphNumber<GRAPHS_TO_CREATE; graphNumber++){
            Graph<Vertex, DefaultEdge> g = createGraph();

            List<String> items = new ArrayList<>();
            items.add("2");
            items.add("1");

            Vertex rootNode = new Vertex("Root", null); 
            g.addVertex(rootNode);

            // The parallelStream will create a new Thread() for each item and call .start with the content of this block?
            items.parallelStream().forEach(item -> {

                // Name our thread
                Thread.currentThread().setName(item);

                for(int i=0; i<NUM_VERTEXES_TO_CREATE; i++){
                    String currentThreadName = Thread.currentThread().getName();
                    String vertexName = "v" + i;
                    Vertex v = new Vertex(vertexName, rootNode);

                    // v.addChild("Vertex added by thread: " + currentThreadName); // Setting children on a new vertex

                    // We need to ensure the key is unique, add in the thread name
                    rootNode.addChild("Child added by thread: " + currentThreadName); // Setting attribute on a shared vertex

                    g.addVertex(v);
                    //g.addEdge(v, rootNode);
                }
            });

            System.out.println(g.toString());
            //exportGraphToImage(g, graphNumber);
        }

    }

    /*
     * Expected outcome / Scenarios:
     * 
     * I expected a graph with 2 vertexes with connections to the root node
     * 
     * The threads will fight over setting attributes on the root node
     *  it may have 2 attributes one for each vertex created..?
     *  alternativley it may only have 1 as the other is overwritten
     * 
     * Scenario 1
     * Thread 1 retrieves the root node, it has no children, it adds an child from Thread 1, it gets written back to the object before Thread 2 looks at the children variable
     * Thread 2 retrieves the root node, it sees the child added from thread 1, it adds an child from Thread 2
     * We end up with 2 children
     * 
     * Scenario 2
     * Thread 1 retrieves the root node, it has no children, it adds a child from Thread 1, and writes it back to the object, meanwhile
     * Thread 2 retrieves the root node, it doesn't see the written children from Thread 1 yet, it adds an child, it saves it to the object. Thread 1 children is overwritten.
     * We end up with 1 child
     * 
     * Initially my datastructure was hashmap and I was adding the same key from different threads, so only 1 key would ever be present.
     * Now changed type to arraylist.
     * 
     * Both the child from thread1 and thread2 get added, thought I don't think this behaviour is guarnteed. I want to see it fail. Then add synch so it works.

     * What happens if I setChildren, vs addChild
     * Obviously, if I call setChildren from thread1, it will overwrite the entire value
     * If I call addChild.. not sure.
     * 
     * Fix vertex with syncrhonsed? + volatile?
     */

    /**
     * Create a toy graph based on String objects.
     *
     * @return a graph based on String objects.
     */
    private static Graph<Vertex, DefaultEdge> createGraph() {
        Graph<Vertex, DefaultEdge> g = new SimpleGraph<>(DefaultEdge.class);
        return g;
    }

    private void exportGraphToImage(Graph<Vertex, DefaultEdge> stringGraph, int result){
        JGraphXAdapter<Vertex, DefaultEdge> graphAdapter = new JGraphXAdapter<Vertex, DefaultEdge>(stringGraph);
        mxIGraphLayout layout = new mxCircleLayout(graphAdapter);
        layout.execute(graphAdapter.getDefaultParent());

        File imgFile = new File("C:/Users/samsn/Desktop/Software projects/MultithreadedInMemoryGraph/demo/graph"+result+".png");
        try {
            imgFile.createNewFile();
            BufferedImage image = mxCellRenderer.createBufferedImage(graphAdapter, null, 5, Color.WHITE, true, null);
            ImageIO.write(image, "PNG", imgFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Vertex {
    private Vertex parent;
    private String label;
    private List<String> children = new ArrayList<>();

    public Vertex(String label, Vertex parent) {
        this.label = label;
        this.parent = parent;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
    
    public synchronized void addChild(String child){ // Without synchronized number of children added to the root node is inconsistent
        this.children.add(child);
    }

    public List<String> getChildren() {
        return children;
    }

    public void setChildren(List<String> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        System.out.println("Children length: " + children.size());
        return "Vertex [label=" + label + ", children=" + children + "] \n";
    }
}