# FinSplit — Java Edition

A beginner-friendly Java Swing desktop prototype for personal finance and shared bill splitting.

## Features
- Dashboard showing income, expenses, and balance
- Add and remove income and expense entries
- Add budget categories and limits
- Split a bill equally among a number of people
- Track savings goals
- Basic income/expense report
- Local persistence in `finsplit.dat`

## Requirements
Java JDK 17 or later.

## Run on Windows
Open Command Prompt in this folder and run:

```bat
javac -d out src\com\finsplit\FinSplitApp.java
java -cp out com.finsplit.FinSplitApp
```

The app is a desktop Swing application; GitHub hosts the source but does not run the GUI in the browser. This is a learning prototype, not a production financial application.
