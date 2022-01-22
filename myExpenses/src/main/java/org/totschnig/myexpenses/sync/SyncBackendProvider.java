package org.totschnig.myexpenses.sync;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.annimon.stream.Exceptional;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.sync.json.AccountMetaData;
import org.totschnig.myexpenses.sync.json.ChangeSet;
import org.totschnig.myexpenses.sync.json.TransactionChange;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface SyncBackendProvider {

  void withAccount(Account account) throws IOException;

  void resetAccountData(String uuid) throws IOException;

  void lock() throws IOException;

  void unlock() throws IOException;

  @NonNull
  Optional<ChangeSet> getChangeSetSince(SequenceNumber sequenceNumber, Context context) throws IOException;

  @NonNull
  SequenceNumber writeChangeSet(SequenceNumber lastSequenceNumber, List<TransactionChange> changeSet, Context context) throws IOException;

  @NonNull
  Stream<Exceptional<AccountMetaData>> getRemoteAccountStream() throws IOException;

  void setUp(AccountManager accountManager, android.accounts.Account account, @Nullable String encryptionPassword, boolean create) throws Exception;

  void storeBackup(Uri uri, String fileName) throws IOException;

  @NonNull
  List<String> getStoredBackups() throws IOException;

  InputStream getInputStreamForBackup(String backupFile) throws IOException;

  void initEncryption() throws GeneralSecurityException, IOException;

  void updateAccount(Account account) throws IOException;

  Exceptional<AccountMetaData> readAccountMetaData();

  class SyncParseException extends Exception {
    public SyncParseException(Exception e) {
      super(e.getMessage(), e);
    }

    public SyncParseException(String message) {
      super(message);
    }
  }

  class AuthException extends IOException {
    public @Nullable Intent resolution;

    public AuthException(Throwable cause, @Nullable Intent intent) {
      super(cause);
      this.resolution = intent;
    }
  }

  class EncryptionException extends Exception {
    public static EncryptionException notEncrypted(Context context) {
      return new EncryptionException(context.getString(R.string.sync_backend_is_not_encrypted));
    }

    public static EncryptionException encrypted(Context context) {
      return new EncryptionException(context.getString(R.string.sync_backend_is_encrypted));
    }

    public static EncryptionException wrongPassphrase(Context context) {
      return new EncryptionException(context.getString(R.string.sync_backend_wrong_passphrase));
    }

    private EncryptionException(String message) {
      super(message);
    }
  }
}
