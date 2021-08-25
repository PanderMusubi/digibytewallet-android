package io.digibyte.tools.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TransactionDao {

    @Query("SELECT * FROM digi_transaction")
    List<DigiTransaction> getAll();

    @Query("SELECT * FROM digi_transaction WHERE tx_hash LIKE :txHash LIMIT 1")
    DigiTransaction findByTxHash(String txHash);

    @Insert
    void insertAll(DigiTransaction... transactions);

    @Delete
    void delete(DigiTransaction user);
}