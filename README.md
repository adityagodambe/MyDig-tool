# MyDig-tool
Custom written tool that emulates the linux "dig" command

This is custom made "dig" tool written in Java that performs DNS resolution iteratively.
It uses the dnsjava library to perform top level DNS query resolving followed by iterative DNS resolution based on the returned list of top level domains.

## Usage:

Download the dnsjava zip from [here](http://www.dnsjava.org/download/dnsjava-2.1.8.zip)

Unzip the file and move MyDig.java into the extracted folder.

Compile the program using `javac MyDig.java` using terminal in the same folder.

Run the command `java MyDig <hostname_to_be_resolved> <type>`
