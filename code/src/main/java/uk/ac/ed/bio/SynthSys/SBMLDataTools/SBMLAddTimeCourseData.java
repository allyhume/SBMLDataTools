/**
 * For license details see associated LICENSE.txt file.
 */

package uk.ac.ed.bio.synthsys.SBMLDataTools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBMLWriter;

import com.ctc.wstx.exc.WstxUnexpectedCharException;
import com.opencsv.CSVReader;

/**
 * Command line program to add external time course data to an SBML model.
 * 
 * @author Ally Hume
 */
public class SBMLAddTimeCourseData {
    
    // Strings for the various command line options
    private static final String OPTION_CSV_IN        = "csvIn";
    private static final String OPTION_CSV_OUT       = "csvOut";
    private static final String OPTION_SBML_IN       = "sbmlIn";
    private static final String OPTION_SBML_LEVEL    = "sbmlLevel";
    private static final String OPTION_SBML_VERSION  = "sbmlVersion";
    private static final String OPTION_SBML_OUT      = "sbmlOut";
    private static final String OPTION_HELP          = "help";
    private static final String OPTION_CSV_SEPARATOR = "csvSeparator";
    private static final String PROGRAM_NAME         = "SBMLAddTimeCourseData";
    
    // Defaults for any SBML files we create
    private static final int    DEFAULT_SBML_LEVEL   = 3;
    private static final int    DEFAULT_SBML_VERSION = 1;
    private static final int    DEFAULT_NUM_INTERVALS = 10;
    

    /**
     * Main command line call.
     * 
     * @param args  command line arguments
     * 
     * @throws IOException if an unexpected IO error occurs. Most common errors are reported nicer
     *                     than throwing an exception.
     */
    public static void main(String[] args) throws IOException {
        
        Options options = getCommandLineOptions();

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(options, args);
            
            // Handle help option
            if (commandLine.hasOption(OPTION_HELP)) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp( PROGRAM_NAME, options );
                return;
            }

            // Everything should be in the options so if there are any any left then we have an 
            // error
            if (commandLine.getArgs().length != 0) {
                String error = "Usage error: unexpected arguments:";
                for (String s : commandLine.getArgList()) {
                    error = error + " " + s;
                }
                throw new ParseException(error);
            }
            
            // One of sbmlOut or csvOut is required
            if (!commandLine.hasOption(OPTION_SBML_OUT) && !commandLine.hasOption(OPTION_CSV_OUT)) {
                throw new ParseException("One of sbmlOut or csvOut arguments is required.");
            }
            
            // Get the CSV in file
            Reader csvInReader;
            if (commandLine.hasOption(OPTION_CSV_IN)) {
                String fileName = commandLine.getOptionValue(OPTION_CSV_IN);
                csvInReader = new BufferedReader(new FileReader(fileName));
            } else {
                // Read from stdin
                csvInReader = new BufferedReader(new InputStreamReader(System.in));
            }
            
            // Get SBML in reader
            SBMLDocument doc;
            if (commandLine.hasOption(OPTION_SBML_IN)) {
                File file= new File(commandLine.getOptionValue(OPTION_SBML_IN));
                doc = SBMLReader.read(file);
            } else {
                // Create an empty SBML model
                int level   = getIntegerOption(commandLine, OPTION_SBML_LEVEL, DEFAULT_SBML_LEVEL);
                int version = getIntegerOption(commandLine, OPTION_SBML_VERSION, DEFAULT_SBML_VERSION);
                doc = new SBMLDocument(level, version);
                doc.createModel("model");
            }
            
            // Get SBML out file
            File sbmlOutFile = new File(commandLine.getOptionValue(OPTION_SBML_OUT));

            // CSV file out
            BufferedWriter csvOutWriter = null;
            if (commandLine.hasOption(OPTION_CSV_OUT)) {
                File csvFileOut = new File(commandLine.getOptionValue(OPTION_CSV_OUT));
                csvOutWriter = new BufferedWriter(new FileWriter(csvFileOut));
            }

            // Do the work
            process(csvInReader, doc.getModel(), csvOutWriter, getSeparator(commandLine));
            
            csvInReader.close();
            if (csvOutWriter != null) csvOutWriter.close();
            
            // Write the SBML file out
            if (commandLine.hasOption(OPTION_SBML_OUT)) {
                SBMLWriter.write(doc, sbmlOutFile, "SBMLAddTimeCourseData", "1.0");
            }
            
        }
        catch( ParseException e) {
            System.err.println("Error: " + e.getLocalizedMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( PROGRAM_NAME, options );
        }
        catch( FileNotFoundException e) {
            System.err.println("Error: File not found: " + e.getLocalizedMessage());
        }
        catch( WstxUnexpectedCharException e) {
            System.err.println("Error reading SBML file: " + e.getLocalizedMessage());
        }
        catch( XMLStreamException e) {
            System.err.println("Error reading SBML file: " + e.getLocalizedMessage());
        }
        catch( IllegalArgumentException e ) {
            System.err.println("Error: " + e.getLocalizedMessage());
        }
    }

    /**
     * Reads the CSV data from the given reader, validates it and writes it into the SBML model
     * as a parameter with an assignment rule.
     * 
     * @param reader     csv data reader
     * @param model      SBML model
     * @param writer     csv writer, or null
     * @param separator  csv data separator
     * 
     * @throws IOException if an unexpected IO error occurs.
     */
    public static void process(Reader reader, Model model, BufferedWriter csvWriter, char separator) 
            throws IOException {
        // Read CSV
        CSVReader csvReader = new CSVReader(reader, separator);
        List<String[]> csvData = csvReader.readAll();
        csvReader.close();

        validateCsvData(csvData);
        
        // Get the number of columns
        int numCols = csvData.get(0).length;
        
        List<Double> fittedTimes = null;
        List<List<Double>> fittedValues = new ArrayList<List<Double>>();

        // If we are to output CSV data then calculate the times we output
        if (csvWriter != null) {
            fittedTimes  = new ArrayList<Double>();
            for (int row=1; row<csvData.size()-1; ++row) {
                double t1 = Double.parseDouble((csvData.get(row)[0]));
                double t2 = Double.parseDouble((csvData.get(row+1)[0]));
                double interval = (t2-t1)/(double) DEFAULT_NUM_INTERVALS;
                for (int i=0; i<DEFAULT_NUM_INTERVALS; ++i) {
                    fittedTimes.add(t1+interval*i);
                }
            }
            // Add the last time
            fittedTimes.add(Double.parseDouble((csvData.get(csvData.size()-1)[0])));
        }
        
        // Assume time is column 0, process each other column in turn
        for (int col=1; col<numCols; ++col) {

            // Assume row 0 is a header
            String paramName = csvData.get(0)[col];

            // Collect the times and values for this column
            List<Double> times  = new ArrayList<Double>();
            List<Double> values = new ArrayList<Double>();
            
            for (int row=1; row<csvData.size(); ++row) {
                String[] rowData = csvData.get(row);
                times.add(Double.parseDouble(rowData[0]));
                values.add(Double.parseDouble(rowData[col]));
            }
            
            List<Double> fittedValuesForThisColumn = null;
            if (fittedTimes != null) {
                fittedValuesForThisColumn = new ArrayList<Double>();
            }
            fittedValues.add(fittedValuesForThisColumn);
                
            // Add the data to the SBML model
            SBMLTimeCourseDataHelper.addParameterUsingCubicSpline(
                    model, paramName, times, values, fittedTimes, fittedValuesForThisColumn);
        }
        
        // Now we can write the CSV data
        if (csvWriter != null) {
            // Write the header
            for (int col=0; col<numCols; ++col) {
                if (col != 0) csvWriter.write(separator);
                csvWriter.write(csvData.get(0)[col]);
            }
            csvWriter.newLine();
            // Write the data
            for (int row=0; row<fittedTimes.size(); ++row) {
                // Time
                csvWriter.write(Double.toString(fittedTimes.get(row)));
                for (List<Double> fittedValuesForColumn : fittedValues) {
                    csvWriter.write(separator);
                    csvWriter.write(Double.toString(fittedValuesForColumn.get(row)));
                }
                csvWriter.newLine();
            }
        }
    }
    
   
    /**
     * Gets the command line options.  This includes details to get their arguments in necessary
     * and descriptions required to display help message.
     * 
     * @return command line options
     */
    private static Options getCommandLineOptions() {
        Options options = new Options();
        
        Option option;
        
        // help
        option = Option.builder(OPTION_HELP).build();
        option.setDescription("Displays this help message");
        options.addOption(option);

        // csvIn
        option = Option.builder(OPTION_CSV_IN).hasArg(true).argName("file").build();
        option.setDescription(
                "csv time course data file. Optional: if not specified stdin will be used.");
        options.addOption(option);
        
        // csvOut
        option = Option.builder(OPTION_CSV_OUT).hasArg(true).argName("file").build();
        option.setDescription(
                "csv file to write fitted data. Optional.");
        options.addOption(option);

        // sbmlIn
        option = Option.builder(OPTION_SBML_IN).hasArg(true).argName("file").build();
        option.setDescription(
                "Input SBML file. Optional: if not specified an empty model will be used.");
        options.addOption(option);
        
        // sbmlLevel
        option = Option.builder(OPTION_SBML_LEVEL).hasArg(true).argName("level").build();
        option.setDescription(
                "SBML level of SBML model if no SBML input file is specified. Optional. " + 
                        "Default is " + DEFAULT_SBML_LEVEL);
        options.addOption(option);
        
        // sbmlVerison
        option = Option.builder(OPTION_SBML_VERSION).hasArg(true).argName("version").build();
        option.setDescription(
                "SBML version of SBML model if no SBML input file is specified. Optional. " + 
                        "Default is " + DEFAULT_SBML_VERSION);
        options.addOption(option);

        // sbmlOut 
        option = Option.builder(OPTION_SBML_OUT).hasArg(true).argName("file").build();
        option.setDescription(
                "Output SBML file. Optional.");
        options.addOption(option);
        
        // csvSeparator
        option = Option.builder(OPTION_CSV_SEPARATOR).hasArg(true).argName("separator").build();
        option.setDescription(
                "Single character separator used between fields of CSV file " + 
                        "(or TAB can be used for a tab character). " + 
                        "Optional. Default is comma ','.");
        options.addOption(option);

        return options;
    }
    
    /**
     * Validates the input CSV data to ensure it has the properties required. The data is expected
     * to have a header row and at least three data rows. The first column is time values and
     * must be in ascending order. There must be at least one other data column.
     * 
     * @param csvData CSV data
     * 
     * @throws IllegalArgumentException if the data is invalid.
     */
    private static void validateCsvData(List<String[]> csvData) {
        
        // Must have header row and at least 3 data rows
        if (csvData.size() < 4 ) {
            throw new IllegalArgumentException(
                    "Input CSV data must have header row and at least 3 data rows");
        }
        
        int numColumns = csvData.get(0).length;

        if (numColumns < 2 ) {
            throw new IllegalArgumentException(
                    "Input CSV data must have time column and at least one data column");
        }

        double lastTime = getCSVDataValue(csvData, 1, 0);
        
        for (int row=1; row < csvData.size(); row++) {            
            // Ensure consistent number of columns
            if ( csvData.get(row).length != numColumns) {
                throw new IllegalArgumentException(
                        "Input CSV data must have same number of columns in each row. " +
                        "row " + (row+1) + " has " + csvData.get(row).length + 
                        " columns, expected it to have " + numColumns);
            }
            
            // Ensure all data is double
            for ( int col = 0; col < numColumns; ++col) {
                getCSVDataValue(csvData, row, col);
            }
            
            // Ensure time is always ascending
            double time;
            if (row > 1 ) {
                time = Double.parseDouble(csvData.get(row)[0]);
                if (time <= lastTime) throw new IllegalArgumentException(
                        "Input CSV data must be sorted with ascending time. The time in " +
                        "row " + (row+1) + " (" + time + ") is before time in row " +
                        row + " (" + lastTime + ").");
                lastTime = time;
            }
        }
    }
    
    /**
     * Gets the numeric value in the specified row and column of the CSV data
     * 
     * @param csvData CSV data
     * @param row     row index (zero based)
     * @param col     col index (zero based)
     * 
     * @return the numeric value in the specified position
     * 
     * @throws IllegalArgumentException if CSV value is non-numeric. The exception contains a user
     *                                  friendly error message than can be displayed to the user.
     */
    private static double getCSVDataValue(List<String[]> csvData, int row, int col) {
        try {
            return Double.parseDouble(csvData.get(row)[col]);
        }
        catch(NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Input CSV data in row " + (row+1) + ", column " + (col+1) + 
                    " is not a numerical value: " + csvData.get(row)[col]);
        }
    }
    
    /**
     * Gets the value of an integer command line option.
     * 
     * @param commandLine    parsed command line 
     * @param optionName     name of the option
     * @param defaultValue   default value if the option is not specified
     * 
     * @return the option value, or the default value if it is not specified
     * 
     * @throws IllegalArgumentException if the specified option value is not an integer. The
     *                                  error message is user friendly and can be displayed to
     *                                  the user.
     */
    private static int getIntegerOption(
            CommandLine commandLine, String optionName, int defaultValue) {
        
        if (commandLine.hasOption(optionName)) {
            try {
                return Integer.parseInt(commandLine.getOptionValue(optionName));
            }
            catch(NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Command line option " + optionName + " must be an integer", e);
            }
        }
        return defaultValue;
    }    
    
    /**
     * Gets the CSV separator character from the command line.
     * 
     * @param commandLine parsed command line
     * 
     * @return the specified CSV separator character, or the default is the option is not specified.
     * 
     * @throws IllegalArgumentException if the option value is invalid. The exception has a user-
     *                                  friendly message than can be displayed to the user.
     */
    private static char getSeparator(CommandLine commandLine) {
        if (commandLine.hasOption(OPTION_CSV_SEPARATOR)) {
            String separator = commandLine.getOptionValue(OPTION_CSV_SEPARATOR);
            if (separator.toUpperCase().equals("TAB")) return '\t';
            if (separator.length() != 1) {
                throw new IllegalArgumentException(
                        "csvSeparator must be a single character (or TAB)");
            }
            return separator.charAt(0);
        }
        return ',';
    }
}
