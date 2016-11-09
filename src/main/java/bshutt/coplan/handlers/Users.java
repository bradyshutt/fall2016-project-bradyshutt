package bshutt.coplan.handlers;

import bshutt.coplan.DBException;
import bshutt.coplan.Database;
import bshutt.coplan.Handler;
import bshutt.coplan.models.User;
import com.mongodb.MongoWriteException;
import org.bson.Document;

import java.util.ArrayList;

public class Users {

    private Database db = Database.getInstance();

    public Handler getUser = (req, res) -> {
        String username = req.get("username");
        User user = null;
        try {
            user = new User().load(username);
            if (user == null)
                res.err("User '" + username + "' not found!", req);
            else
                res.setResponse(user.getAttributes());
        } catch (DBException exc) {
            res.err(exc, req);
        }
    };

    public Handler createUser = (req, res) -> {
        User user = new User().build(req.data);
        try {
            if (!User.isUsernameAvailable(user.username)) {
                res.err("Username '" + user.username + "' is not available.", req);
                return;
            }
            if (user.validate()) {
                user.save();
                res.append("createdUser", "success");
                res.end();
            } else {
                res.err("Invalid data params for creating a user", req);
            }
        } catch (DBException exc) {
            res.err(exc);
        }
    };

    public Handler usernameIsAvailable = (req, res) -> {
        String username = req.get("username");
        Document userDoc = null;
        try {
            userDoc = this.db
                    .col("users")
                    .find(db.filter("username", username))
                    .first();
        } catch (DBException exc) {
            res.err(exc, req);
        }
        res.append("usernameIsAvailable", (userDoc == null));
        res.end();
    };

    public Handler authenticate = (req, res) -> {
        try {
            User user = new User().load(req.get("username"));
            if (user.authenticate(req.get("password")))
                res.setResponse("Authentication successful");
            else
                res.setResponse("Incorrect password");
        } catch (DBException exc) {
            res.err(exc, req);
        }
    };

}

//    public Document getUser(String username) {
//        Document userDoc = null;
//        try {
//            userDoc = this.db
//                    .col("users")
//                    .find(db.filter("username", username))
//                    .first();
//        } catch (DBException exc) {
//            res.err(exc, res);
//        }
//        return userDoc;
//    }

//    public void createUser(Document user) {
//        try {
//            this.db.col("users").insertOne(user);
//        } catch (DBException exc) {
//            res.
//            e.printStackTrace();
//        }
//    }

