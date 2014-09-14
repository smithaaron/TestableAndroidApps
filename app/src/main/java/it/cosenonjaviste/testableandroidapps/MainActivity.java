package it.cosenonjaviste.testableandroidapps;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.parceler.ParcelClass;
import org.parceler.ParcelClasses;

import javax.inject.Inject;
import javax.inject.Singleton;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnItemClick;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import it.cosenonjaviste.testableandroidapps.base.ActivityContextBinder;
import it.cosenonjaviste.testableandroidapps.base.BundleObjectSaver;
import it.cosenonjaviste.testableandroidapps.base.ObjectGraphHolder;
import it.cosenonjaviste.testableandroidapps.base.RxMvcActivity;
import it.cosenonjaviste.testableandroidapps.model.Owner;
import it.cosenonjaviste.testableandroidapps.model.Repo;
import it.cosenonjaviste.testableandroidapps.model.RepoResponse;
import it.cosenonjaviste.testableandroidapps.mvc.RepoListController;
import it.cosenonjaviste.testableandroidapps.mvc.RepoListModel;
import it.cosenonjaviste.testableandroidapps.mvc.RepoService;
import it.cosenonjaviste.testableandroidapps.share.ShareHelper;

@ParcelClasses({@ParcelClass(RepoResponse.class), @ParcelClass(Repo.class), @ParcelClass(Owner.class), @ParcelClass(RepoListModel.class)})
public class MainActivity extends RxMvcActivity<RepoListModel> {

    @InjectView(R.id.list) ListView listView;

    @InjectView(R.id.query) EditText query;

    @InjectView(R.id.progress) View progress;

    @InjectView(R.id.reload) View reload;

    @Inject WelcomeDialogManager welcomeDialogManager;

    @Inject RepoListController repoListController;

    @Inject ShareHelper shareHelper;

    private RepoAdapter repoAdapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ObjectGraph appObjectGraph = ObjectGraphHolder.getObjectGraph(getApplication());
        ObjectGraph activityObjectGraph = appObjectGraph.plus(new ActivityModule(this));
        activityObjectGraph.inject(this);

        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

        repoAdapter = new RepoAdapter(this);
        repoListController.loadFromBundle(new BundleObjectSaver<RepoListModel>(savedInstanceState, "model"));

        listView.setAdapter(repoAdapter);

        welcomeDialogManager.showDialogIfNeeded();
    }

    @OnItemClick(R.id.list) void shareItem(int position) {
        Repo repo = repoAdapter.getItem(position);
        repoListController.toggleStar(new ActivityContextBinder(this), repo);
//        shareHelper.share(repo.getName(), repo.getName() + " " + repo.getUrl());
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        repoListController.saveInBundle(new BundleObjectSaver(outState, "model"));
    }

    @Override protected RepoListController getController() {
        return repoListController;
    }

    @Override public void updateView(RepoListModel model) {
        if (model.isProgressVisible()) {
            progress.setVisibility(View.VISIBLE);
            reload.setVisibility(View.GONE);
            listView.setVisibility(View.GONE);
        } else if (model.isReloadVisible()) {
            reload.setVisibility(View.VISIBLE);
            progress.setVisibility(View.GONE);
        } else {
            listView.setVisibility(View.VISIBLE);
            progress.setVisibility(View.GONE);
            reload.setVisibility(View.GONE);
        }
        repoAdapter.reloadData(model.getRepos(), model.getUpdatingRepos());
        if (!TextUtils.isEmpty(model.getExceptionMessage())) {
            Toast.makeText(MainActivity.this, model.getExceptionMessage(), Toast.LENGTH_LONG).show();
            model.setExceptionMessage(null);
        }
    }

    @OnEditorAction(R.id.query) boolean onSearch(int actionId) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            executeSearch();
            return true;
        }
        return false;
    }

    @OnClick({R.id.search, R.id.reload}) void executeSearch() {
        String queryString = query.getText().toString();
        repoListController.listRepos(new ActivityContextBinder(this), queryString);
    }

    @Module(injects = MainActivity.class, addsTo = AppModule.class)
    public static class ActivityModule {
        private FragmentActivity activity;

        public ActivityModule(FragmentActivity activity) {
            this.activity = activity;
        }

        @Provides @Singleton public FragmentActivity provideActivity() {
            return activity;
        }

        @Provides @Singleton RepoListController provideController(RepoService repoService) {
            return new RepoListController(repoService);
        }
    }
}
