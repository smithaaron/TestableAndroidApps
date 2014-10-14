package it.cosenonjaviste.testableandroidapps.mvc;

import it.cosenonjaviste.testableandroidapps.model.Repo;
import it.cosenonjaviste.testableandroidapps.model.RepoService;
import it.cosenonjaviste.testableandroidapps.mvc.base.PresenterArgs;
import it.cosenonjaviste.testableandroidapps.mvc.base.RxMvpPresenter;

public class RepoListPresenter extends RxMvpPresenter<RepoListModel> {

    private RepoService repoService;

    public RepoListPresenter(RepoService repoService) {
        this.repoService = repoService;
    }

    protected RepoListModel createModel(PresenterArgs args) {
        return new RepoListModel();
    }

    public void listRepos(String queryString) {
        subscribePausable(repoService.listRepos(queryString),
                () -> view.showProgress(null),
                repos -> {
                    model.setReloadVisible(false);
                    model.setRepos(repos);
                    notifyModelChanged();
                }, throwable -> {
                    model.setReloadVisible(true);
                    notifyModelChanged();
                });
    }

    public void toggleStar(Repo repo) {
        model.getUpdatingRepos().add(repo.getId());
        notifyModelChanged();
        subscribePausable(repoService.toggleStar(repo), repo1 -> {
        }, e -> {
            model.getUpdatingRepos().remove(repo.getId());
            model.setExceptionMessage(e.getMessage());
            notifyModelChanged();
        }, () -> {
            model.getUpdatingRepos().remove(repo.getId());
            notifyModelChanged();
        });
    }
}
