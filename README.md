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

## Command line application

The SBMLAddTimeCourseData tool adds time course data to an SBML file. The time course
data is added as a parameter specifed by an assignment rule.  The assignment rules
describes the data using a piecewise function obtained by fitting cubic splines to
the data.

![Raw data plot](https://raw.github.com/allyhume/SBMLDataTools/master/images/rawTempPlot.png)

![Fitted plot](https://raw.github.com/allyhume/SBMLDataTools/master/images/fittedPlot.png)

http://www.erh.noaa.gov/pbz/hourlywx/hr_pit_15.06



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
