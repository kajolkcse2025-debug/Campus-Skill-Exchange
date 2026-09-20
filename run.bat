@echo off
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -d out -nowarn @sources.txt
del sources.txt
java -cp out Main %*
