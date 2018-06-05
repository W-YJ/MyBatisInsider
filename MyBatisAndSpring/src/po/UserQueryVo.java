package po;

import java.util.List;

public class UserQueryVo {
    private User user;
    private UserCustom userCustom;
    private List<Integer> ids;

    public void setIds(List<Integer> ids){
        this.ids = ids;
    }

    public List<Integer> getIds() {
        return ids;
    }


    public User getUser() {
        return user;
    }

    public void setUser(User userl) {
        this.user = user;
    }

    public UserCustom getUserCustom() {
        return userCustom;
    }

    public void setUserCustom(UserCustom userCustom) {
        this.userCustom = userCustom;
    }
}
