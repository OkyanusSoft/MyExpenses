package org.totschnig.myexpenses.activity;

import android.accounts.AccountManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.Toast;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.dialog.SetupWebdavDialogFragment;
import org.totschnig.myexpenses.fragment.SyncBackendList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.sync.WebDavBackendProviderFactory;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.Result;

import java.io.File;

import static org.totschnig.myexpenses.sync.WebDavBackendProvider.KEY_WEB_DAV_CERTIFICATE;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_CREATE_SYNC_ACCOUNT;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SYNC_LINK_LOCAL;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SYNC_LINK_REMOTE;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SYNC_REMOVE_BACKEND;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_SYNC_UNLINK;
import static org.totschnig.myexpenses.task.TaskExecutionFragment.TASK_WEBDAV_TEST_LOGIN;

public class ManageSyncBackends extends ProtectedFragmentActivity implements
    EditTextDialog.EditTextDialogListener {

  private Account newAccount;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeIdEditDialog());
    super.onCreate(savedInstanceState);
    setContentView(R.layout.manage_sync_backends);
    setupToolbar(true);
    setTitle(R.string.pref_manage_sync_backends_title);
  }

  //LocalFileBackend
  @Override
  public void onFinishEditDialog(Bundle args) {
    String filePath = args.getString(EditTextDialog.KEY_RESULT);
    File baseFolder = new File(filePath);
    if (!baseFolder.isDirectory()) {
      Toast.makeText(this, "No directory " + filePath, Toast.LENGTH_SHORT).show();
      return;
    }
    String accountName = args.getString(GenericAccountService.KEY_SYNC_PROVIDER_LABEL) + " - "
        + filePath;
    Bundle bundle = new Bundle(2);
    bundle.putString(GenericAccountService.KEY_SYNC_PROVIDER_ID,
        args.getString(GenericAccountService.KEY_SYNC_PROVIDER_ID));
    bundle.putString(GenericAccountService.KEY_SYNC_PROVIDER_URL, filePath);
    createAccount(accountName, null, bundle);
  }

  //WebDav
  public void onFinishWebDavSetup(Bundle data) {
    String userName = data.getString(AccountManager.KEY_ACCOUNT_NAME);
    String password = data.getString(AccountManager.KEY_PASSWORD);
    String url = data.getString(GenericAccountService.KEY_SYNC_PROVIDER_URL);
    String certificate = data.getString(KEY_WEB_DAV_CERTIFICATE);
    String accountName = WebDavBackendProviderFactory.LABEL + " - " + url;

    Bundle bundle = new Bundle();
    bundle.putString(GenericAccountService.KEY_SYNC_PROVIDER_ID, String.valueOf(R.id.CREATE_BACKEND_WEBDAV_COMMAND));
    bundle.putString(GenericAccountService.KEY_SYNC_PROVIDER_URL, url);
    bundle.putString(GenericAccountService.KEY_SYNC_PROVIDER_USERNAME, userName);
    if (certificate != null) {
      bundle.putString(KEY_WEB_DAV_CERTIFICATE, certificate);
    }
    createAccount(accountName, password, bundle);
  }

  private void createAccount(String accountName, String password, Bundle bundle) {
    Bundle args = new Bundle();
    args.putString(AccountManager.KEY_ACCOUNT_NAME, accountName);
    args.putString(AccountManager.KEY_PASSWORD, password);
    args.putParcelable(AccountManager.KEY_USERDATA, bundle);
    getSupportFragmentManager()
        .beginTransaction()
        .add(TaskExecutionFragment.newInstanceWithBundle(args, TASK_CREATE_SYNC_ACCOUNT), ProtectionDelegate.ASYNC_TAG)
        .commit();
  }

  @Override
  public void onCancelEditDialog() {

  }

  @Override
  public void onPositive(Bundle args) {
    switch (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE)) {
      case R.id.SYNC_UNLINK_COMMAND: {
        startTaskExecution(TASK_SYNC_UNLINK,
            new String[]{args.getString(DatabaseConstants.KEY_UUID)}, null, 0);
        break;
      }
      case R.id.SYNC_REMOVE_BACKEND_COMMAND: {
        startTaskExecution(TASK_SYNC_REMOVE_BACKEND,
            new String[]{args.getString(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME)}, null, 0);
        break;
      }
    }
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    switch (command) {
      case R.id.SYNC_LINK_COMMAND_DO_LOCAL: {
        Account account = getListFragment().getAccountForSync((Long) tag);
        startTaskExecution(TASK_SYNC_LINK_LOCAL,
            new String[]{account.uuid}, account.getSyncAccountName(), 0);
        break;
      }
      case R.id.SYNC_LINK_COMMAND_DO_REMOTE: {
        Account account = getListFragment().getAccountForSync((Long) tag);
        startTaskExecution(TASK_SYNC_LINK_REMOTE,
            null, account, 0);
        break;
      }
    }
    return super.dispatchCommand(command, tag);
  }

  @Override
  public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    Result result = (Result) o;
    switch (taskId) {
      case TASK_WEBDAV_TEST_LOGIN: {
        getWebdavFragment().onTestLoginResult(result);
        break;
      }
      case TASK_CREATE_SYNC_ACCOUNT:
      case TASK_SYNC_REMOVE_BACKEND: {
        if (result.success) {
          getListFragment().reloadAccountList();
        }
        break;
      }
      case TASK_SYNC_UNLINK:
      case TASK_SYNC_LINK_LOCAL:
      case TASK_SYNC_LINK_REMOTE: {
        if (result.success) {
          getListFragment().reloadLocalAccountInfo();
        }
        break;
      }
    }
  }

  private SyncBackendList getListFragment() {
    return (SyncBackendList) getSupportFragmentManager().findFragmentById(R.id.backend_list);
  }

  private SetupWebdavDialogFragment getWebdavFragment() {
    return (SetupWebdavDialogFragment) getSupportFragmentManager().findFragmentByTag(
        WebDavBackendProviderFactory.WEBDAV_SETUP);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.SYNC_DOWNLOAD_COMMAND:
        newAccount = getListFragment().getAccountForSync(
            ((ExpandableListContextMenuInfo) item.getMenuInfo()).packedPosition);
        startDbWriteTask(false);
        return true;
    }
    return super.onContextItemSelected(item);
  }

  @Override
  public void onPostExecute(Object result) {
    super.onPostExecute(result);
    newAccount.requestSync();
  }

  @Override
  public Model getObject() {
    return newAccount;
  }

}
