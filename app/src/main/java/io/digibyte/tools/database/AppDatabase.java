package io.digibyte.tools.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {DigiTransaction.class}, version = 6, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract TransactionDao transactionDao();
}