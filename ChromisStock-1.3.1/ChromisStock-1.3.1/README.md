# ChromisStock
Android based Stock Control app for ChromisPOS

The application is designed to be an offline stock management system for ChromisPOS and potentially other POS systems
such as unicentaPOS.

Stock data is downloaded to a local database on an Android device and can be viewed and changed before uploading changes
to the main ChromisPOS database.

Uses include adding new stock during shopping trips or upon delivery at a location and for performing stocktaking.

The current release only works with MySQL databases and does not support changing data. 

PLEASE NOTE: This release only supports MySQL POS databases. A new module running on the terminal will be needed to support Derby databases. This is on the development plan.

Barcode reading is supported by external applications, a suitable application that uses the camera can be installed
from within the application.

Configuration
-------------
On the settings screen:
Database URL: This should be in the format: jdbc:mysql://192.168.1.1:3306/database
              Change the IP address, port and database to suit your MySQL server.
Currently it must be the server IP address, the server name does not appear to work (bug) 

Future Developments
-------------------
Add code to upload changes to the main POS database
