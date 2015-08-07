/**
 * For license details see associated LICENSE.txt file.
 */
package uk.ac.ed.bio.synthsys.SBMLDataTools;

import java.io.IOException;

/**
 * Default class to run in the executable jar.  Maps a task name onto the appropriate class.
 * This allows the user to be shielded from the full class package names at the command line.
 * 
 * @author Ally Hume
 */
public class DefaultExecutor {

    /**
     * Main method. Simply maps the given parameter name to the appropriate class and calls that
     * class.
     * 
     * @param args arguments
     * 
     * @throws IOException 
     */
    public static final void main(String[] args) throws IOException {
        
        if (args.length < 1 ) {
            System.err.println("Usage error: must specify a task name.");
            printAvailableTasks();
            return;
        }
        
        if (args[0].equalsIgnoreCase("SBMLAddTimeCourseData")) {
            SBMLAddTimeCourseData.main(removeFirstArg(args));
            return;
        }   

        System.err.println("Usage error: unknown task name: " + args[0]);
        printAvailableTasks();
    }
    
    /**
     * Removes the first argument from a list of strings, thus removing the task name.
     * 
     * @param args arguments
     * 
     * @return arguments without the first entry.
     */
    private static final String[] removeFirstArg(String[] args) {
        String[] result = new String[args.length-1];
        System.arraycopy(args, 1, result, 0, result.length);
        return result;
    }
    
    /** 
     * Prints the available task names.
     */
    private static final void printAvailableTasks() {
        System.err.println("  Available tasks are:");
        System.err.println("    - SBMLAddTimeCourseData");
    }
}
