package madgik.exareme.utils.embedded.process;


import org.junit.Test;

/**
 * @author alex
 */
public class MadisProcessTest {

    @Test public void testProccess() throws Exception {
        
        MadisProcess madisProcess =
            new MadisProcess("", "../exareme-tools/madis/src/main/python/madis/src/mterm.py");
        madisProcess.start();
        madisProcess.stop();

    }
}
