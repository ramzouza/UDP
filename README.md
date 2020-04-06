#  HTTP Client/Server  Application implemented using UDP

```
-jar httpc.jar GET http://localhost:8007/
```
```
-jar httpc.jar GET http://localhost:8007/bob.txt/
```
```
-jar httpc.jar POST -h Content_Length:12 -d Hello World! http://localhost:8007/folder5/bob2.txt
```
To test the application, simply run the powershell script: parallelClient.ps1
The script will spawn 1 server and 3 client instance to demonstrate multi client support
