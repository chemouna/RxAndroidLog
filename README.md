# RxAndroidLog
A logger with an extensible API for RxJava 
=======
(This project is still under heavy development and api may change very often and a lot in 
the coming versions).

Debugging RxJava's code is pretty hard, *RxAndroidLog* is a library to help with 
logging rxjava operations by providing an extensible api for logging.

Logging operations can be composed with rxjava's operation and granular control for what to log 
is provided.



Binaries
========

Binaries and dependency information for Maven, Ivy, Gradle and others can be found at [http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Ccom.mounacheikhna).

for Gradle:
```groovy
compile 'com.mounacheikhna:rxAndroidLog:0.2'
```

and for Maven:

```xml
<dependency>
  <groupId>com.mounacheikhna</groupId>
  <artifactId>rxAndroidLog</artifactId>
  <version>0.2</version>
  <type>aar</type>
</dependency>
```

Usage 
=====

Create a logging operator with details of what you want to log, for example here we want 
to show the count of the emitted values,log even numbers and show the count of even numbers.
  
```java  
final OperatorLogging<Integer> log = RxLogging.<Integer>logger().showCount("total")
            .when(x -> x % 2 == 0)
            .showCount("even numbers total")
            .onNext(false)
            .log();

Observable.range(1,20).lift(log)
```
                
                
Want to help?
=============

File new issues to discuss specific aspects of the API or the implementation and to propose new
features or improvements.


Licence
=======
    Copyright (c) 2015 Mouna Cheikhna

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

