@rem Windows batch file for testing ExNihilo in 'stand-alone' mode
@rem robert.e.cranfill@boeing.com

@if "test"%1=="test" goto bad


@rem Look at the following for required files:

@set classpath=build
@set classpath=%classpath%;lib\Robustness_bbn_bbn_HEAD_B10_4_3.jar
@set classpath=%classpath%;lib\Scalability_infoether_utilities.jar
@set classpath=%classpath%;lib\junit.jar
@set classpath=%classpath%;D:\COUGAAR\lib\core.jar
@set classpath=%classpath%;D:\COUGAAR\lib\util.jar
@set classpath=%classpath%;D:\COUGAAR\sys\servlet.jar
@set classpath=%classpath%;D:\COUGAAR\sys\servlet.jar

java  test.org.cougaar.robustness.exnihilo.SystemDesignTest %1 %2 %3 %4 %5 %6 %7 %8 %9

@goto done

:bad
@echo    Calling form:  {annealSec} -en {ENInputBaseName} [-t {testCase#}] [-r [{nTimes}]
@echo              or   {annealSec} {XMLInputFile}        [-t {testCase#}] [-r [{nTimes}]
 
:done
