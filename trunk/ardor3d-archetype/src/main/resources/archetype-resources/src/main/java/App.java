package ${groupId};

import com.ardor3d.example.basic.BoxExample;

/**
 * Dummy example to get you going with something.
 *
 * TODO: to be really valuable, this example shouldn't be setting things up using the ExampleBase,
 * it should do something a little more realistic.
 */
public class App extends BoxExample {
    App() {
	super(null, null);
    }

    public static void main( String[] args )
    {
        start(BoxExample.class);
    }
}
