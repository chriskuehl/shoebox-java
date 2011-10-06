=== Shoebox ===

**Current Status:**
The basic feature set is complete, but additional testing needs to be done to ensure all parts of the application work as expected. In addition, some features need to be polished.

Shoebox is a simple Java archiving system. It is intended for the storage of snapshots.

Shoebox stores files based on user-defined time tiers. For example, you can store daily backups for 7 days, then weekly ones for 30 days, then monthly backups indefinitely.

The feature set of Shoebox is intentionally kept minimal. It is not a backup system. You must combine it with some method of creating the files you want it to archive.

Meta information is stored in a SQLite database, which makes it easy to move the backups around.
