package io.digibyte.tools.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "digi_transaction")
public class DigiTransaction {

    @PrimaryKey(autoGenerate = true)
    private int uid;

    @ColumnInfo(name = "tx_hash")
    private byte[] txHash;

    @ColumnInfo(name = "tx_amount")
    private String txAmount;

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public byte[] getTxHash() {
        return txHash;
    }

    public void setTxHash(byte[] txHash) {
        this.txHash = txHash;
    }

    public String getTxAmount() {
        return txAmount;
    }

    public void setTxAmount(String txAmount) {
        this.txAmount = txAmount;
    }
}
