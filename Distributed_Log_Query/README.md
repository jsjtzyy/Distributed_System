#This project is established for CS425 MP1

# How to build:

#Tested on VM cluster. In the directory of MP1, input:
	
make build
	
#The java files will be compiled in bin folder.Since the log file’s size exceeds the limitation of gitlab, we have already deployed them in each machine(01, 02, … 07).

#------------------

# How to run

#After all the java files are compiled, in the directory of MP1, input:

make run

#The program will start to run.

#------------------

# How to use unit test

#The test program is in another directory named: unit test, we have already deployed it on machine 8. The process of running unit test is identical to that of program on other machine.
#The unit test will generate a file named “TestResult.txt” on machine 8.

#------------------