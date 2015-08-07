# SBMLDataTools

This project contains tools to support adding external time course data to SBML models.
By external data we mean data for quantities that are not part of the output of the 
model but may be essential to the simulation of the model. 

An example of external 
data would be the temperature of the environment. A model could use this temperature 
data to vary the rate of reactions but it is not a quantity the model can output. If
the temperature data has been collected as part of an experiment then these tools allow that data to be 
added to the SBML model so it be easily used in reaction rate equations.

The software is available as either a command line application or as a Java library.

If you find this software useful or wish to recommend changes or additions then please
let me know.  

Ally Hume

Twitter: @ally.hume

Email:   A.Hume@ed.ac.uk

## Installation

For the command line version simply download ```SBMLDataTools-0.0.0-withDependencies.jar``` from the
[Release] (https://github.com/allyhume/SBMLDataTools/releases) section of the project's GitHub page.

This is a Java application so to run it you will need to install Java 1.6 or above.

If you have Java installed and the jar file downloaded you simply run Java with reference to the
jar file as shown in the documentation below.


## Command line application

The SBMLAddTimeCourseData tool adds time course data to an SBML file. The time course
data is added to SBML models as a parameter specifed by an assignment rule.  The assignment rules
describes the data using a piecewise function obtained by fitting cubic splines to
the data.

Assume we have some temperature data that we wish to add to include in an SBML model. We first
need this data in a file in CSV (comma separated values) format.  The first column of this
data must be time values and the second column is the data values, in this case the temperature.
The first row of the data must contain name for the columns. These names will be used as the
parameter names in the SBML model.

In our example data we have the temperature every hour so the first few rows of our data (`data.txt`) looks
like:

```
time,temp
0,60  
1,55  
2,52  
3,51  
4,50  
```


The full data set plot is:
![Raw data plot](https://raw.github.com/allyhume/SBMLDataTools/master/images/rawTempPlot.png)

Now we can use cubic spline interpolation to fit a smooth curve through these data points and
write this curve into an SBML model.  First we can choose to see the just to check that we are
happy with it. The SMBLAddTimeCourseData tool can output a data file that contains the fitted
curve data if you use the `-csvOut` option to specify this output file.  The `-csvIn` option is
used to specifiy the raw data input file.  The command line to run this is:

```
java -jar SBMLDataTools-0.0.0-withDependencies.jar SBMLAddTimeCourseData -csvIn data.txt -csvOut fitted.txt
```

This produces a file (`fitted.txt`) that contains data points for the fitted curve. Plotting this
fitted curve with the orginal data shows how the curve fits the data:

![Fitted plot](https://raw.github.com/allyhume/SBMLDataTools/master/images/fittedPlot.png)

We are happy with this so now it is time to create an SBML file containing this data. To create
a new SBML file containing the temprature data we must specify the name of the file to create
using the `-sbmlOut` option:

```
java -jar SBMLDataTools-0.0.0-withDependencies.jar SBMLAddTimeCourseData -csvIn data.txt -sbmlOut mySbml.xml
```

This will produce an SBML level 3 version 1 model. To choose different version and levels you can specify 
the `-sbmlLevel` and `-sbmlVersion` options. For example, the following produces an SBML level 2 version 4
file:
```
java -jar SBMLDataTools-0.0.0-withDependencies.jar SBMLAddTimeCourseData -csvIn data.txt -sbmlOut mySbml.xml -sbmlLevel 2 -sbmlVersion 4
```

Looking at the XML we can see that it contains our temperature data as a parameter:

```
<listOfParameters>
  <parameter id="temp" constant="false" name="temp"/>
</listOfParameters>
```

and also contains an assignment rule associated with this paramater:

```
    <listOfRules>
      <assignmentRule variable="temp">
        <math xmlns="http://www.w3.org/1998/Math/MathML">        
          <piecewise>
             ...
          </piecewise>
        </math>
      </assignmentRule>
    </listOfRules>
```

The assignment rule is constructed using piecewise functions that each define a portion of the curve.  Note
that the parameter will be undefined outside the range of time values given in the input data.

If you wish to add the external data to an existing SBML file then this is easily done by specifing the
`-sbmlIn` option. SBMLAddTimeCourseData will add to the existing SBML file but will create a new one
with the additional content. The command line is:

```
java -jar SBMLDataTools-0.0.0-withDependencies.jar SBMLAddTimeCourseData -csvIn data.txt -sbmlIn mySbml.txt -sbmlOut myNewSbml.xml
```

If your CSV data is not separated by comma but instead by another character then this can be
specified using the `-csvSeparator` option.  For example, if  `|` is used as the separator then the command line
would be:

```
java -jar SBMLDataTools-0.0.0-withDependencies.jar SBMLAddTimeCourseData -csvIn data.txt -csvSeparator "|"  -sbmlOut mySbml.xml
```

If the separator is a tab character this can sometime be hard to type into the command line. In such cases you
can define the separator using the word `TAB'.  For example,

```
java -jar SBMLDataTools-0.0.0-withDependencies.jar SBMLAddTimeCourseData -csvIn data.txt -csvSeparator TAB  -sbmlOut mySbml.xml
```


## Java library

Documentation of the Java library will go here. Sorry it is not yet done. 

I think the code has good JavaDoc so you can look at that. The class to be
used as a library is uk.ac.ed.bio.synthsys.SBMLDataTools.SBMLTimeCourseDataHelper.
The other classes are part of the command line user interface and are not
considered part of the API (but they do show examples of how to use the API).


## Libraries
This project uses the following libraries: 

- [Apache Commons CLI] (https://commons.apache.org/proper/commons-cli/)
- [Apache Commons Lang] (https://commons.apache.org/proper/commons-lang/)
- [Apache Commons Math] (http://commons.apache.org/proper/commons-math/)
- [JSBML] (http://sbml.org/Software/JSBML) 
- [opencsv] (http://opencsv.sourceforge.net/)
- [junit] (http://junit.org/)

There is no need to download these yourself. They are packaged in the project jar download and are 
pulled automatically from Maven repositories when the software is built.

## License
This project is licensed under the MIT license: 

```
The MIT License (MIT)

Copyright (c) 2015, The University of Edinburgh

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
