package bshutt.coplan.handlers;

import java.util.ArrayList;

import bshutt.coplan.models.Pin;
import com.mongodb.Block;
import org.bson.Document;

import bshutt.coplan.Database;
import bshutt.coplan.Handler;
import bshutt.coplan.exceptions.*;
import bshutt.coplan.models.Course;
import bshutt.coplan.models.User;

public class Users {

    private Database db = Database.getInstance();

    public Handler getUser = (req, res) -> {
        String username = req.getData("username");
        User user = null;
        try {
            user = User.load(username);
            if (user == null) {
                res.err("User '" + username + "' not found!");
            } else {
                res.append("user", user.toClientDoc());
                res.end(true);
            }
        } catch (Exception exc) {
            res.err("Error loading user: " + exc.getMessage(), exc);
        }
    };

    public Handler getUserDetails = (req, res) -> {
        res.append("user", req.getUser().toClientDoc());//.toClientDoc(true));
        res.end(true);
    };

    public Handler createUser = (req, res) -> {
        User user = new User();
        user.deserialize(req.getData());
        if (!User.isUsernameAvailable(user.username)) {
            res.append("message", "Username '"+user.username+"' is not available.");
            res.end(false);
            return;
        }
        if (user.validate(user.serialize())) {
            try {
                user.save();
            } catch (DatabaseException exc) {
                res.err("There was an error saving the user", exc);
                return;
            }

            String jwt = null;
            try {
                jwt = user.giveJwt();
            } catch (DatabaseException exc) {
                res.err("There was an error with the JWT", exc);
                return;
            }

            res.append("message", "User creation successful");
            res.append("jwt", jwt);
            res.append("user", user.toClientDoc());
            //res.append("courses", null);
            res.end(true);
        } else {
            res.err("Invalid data params for creating a user");
        }
    };

    public Handler getUserCourses = (req, res) -> {
        ArrayList<Course> courses = req.getUser().courses;
        ArrayList<Document> courseDocs = Course.toDocList(courses);
        res.append("courses", courseDocs);
        res.end(true);
    };

    public Handler addPinToBoard = (req, res) -> {
        String courseName = req.getData("courseName");
        Course course = null;
        try {
            course = Course.load(courseName);
        } catch (CourseValidationException | DatabaseException exc) {
            res.err("error loading course", exc);
            return;
        }
        Document pinDoc = req.getData("pin", Document.class);
        Pin pin = Pin.fromDoc(pinDoc);

        try {
            course.addPin(pin);
        } catch (DatabaseException | PinValidationException | PinParseException | PinSerializationException exc) {
            res.err("error adding pin to course", exc);
        }

        res.append("course", course.toClientDoc());
        res.end(true);
    };

    public Handler removeUser = (req, res) -> {
        String username = req.getData().getString("username");
        try {
            this.db.col("users").findOneAndDelete(this.db.filter("username", username));
            this.db.col("courses").deleteMany(this.db.filter("registeredUsers", username));
        } catch (DatabaseException exc) {
            res.err("There was an error removing the user", exc);
            return;
        }
        res.end(true);
    };

    public Handler usernameIsAvailable = (req, res) -> {
        String username = req.getData("username");
        Document userDoc = null;
        try {
            userDoc = this.db
                    .col("users")
                    .find(db.filter("username", username))
                    .first();
        } catch (DatabaseException exc) {
            res.err("There was a Database error", exc);
            return;
        }
        res.append("usernameIsAvailable", (userDoc == null));
        res.end();
    };

    public Handler checkJwt = (req, res) -> {
        String jwt = req.getData("jwt");
        Boolean validated = null;
        try {
            validated = User.validateJwt(jwt);
        } catch (JwtException exc) {
            res.err("There was a JWT validation error", exc);
            return;
        }
        if (validated)
            res.end(true);
        else
            res.end(false);
    };

    public Handler login = (req, res) -> {
        String username = req.getData("username");
        String password = req.getData("password");
        User user = null;
        try {
            user = User.login(username, password);
        } catch (InvalidPasswordException ipe) {
            res.append("errorMessage", "Invalid password.").end(false);
            return;
        } catch (UserDoesNotExistException udne) {
            res.append("errorMessage", "User '" + username + "' does not exist.").end(false);
            return;
        }

        String jwt = null;
        try {
            jwt = user.giveJwt();
        } catch (DatabaseException exc) {
            res.err("There was a Database error", exc);
            return;
        }

        res.append("jwt", jwt);
        res.append("user", user.toClientDoc());
        res.end(true);
    };

    public Handler registerForCourse = (req, res) -> {
        User user = req.getUser();
        Course course = null;
        try {
            course = Course.load(req.getData("courseName"));
        } catch (CourseValidationException exc) {
            res.err("Error validating course", exc);
            return;
        } catch (DatabaseException exc) {
            res.err("There was a Database error", exc);
            return;
        }


        try {
            course.registerUser(user.username);
        } catch (CourseRegistrationException exc) {
            res.append("message", "User '" + user.username
                    + "' is already registered for '"
                    + req.getData("courseName") + "'.");
            res.end(false);
            return;
        }

        try {
            user.registerForCourse(course);
        } catch (CourseRegistrationException | CourseValidationException | DatabaseException exc) {
            res.err("There was an error registering for that course.", exc);
            return;
        }

        res.append("message", "User '" + user.username
                + "' registered for course + '"
                + req.getData("courseName") + "'.");
        res.append("user", user.toClientDoc());
        res.end(true);
    };

    public Handler unregisterForCourse = (req, res) -> {
        User user = null;
        user = req.getUser();

        Course course = null;
        try {
            course = Course.load(req.getData("courseName"));
        } catch (CourseValidationException | DatabaseException exc) {
            res.err("Error loading course course: " + exc.getMessage(), exc);
            return;
        }

        try {
            user.unregisterForCourse(req.getData("courseName"));
            course.unregisterUser(req.getData("username"));
        } catch (CourseRegistrationException | DatabaseException exc) {
            res.err("Error unregistering for that course: " + exc.getMessage(), exc);
            return;
        }
        res.append("message", "User '" + user.username
                + "' unregistered for courses '"
                + req.getData("courseName") + "'.");
        //res.append("course", course.toClientDoc(true));
        res.append("user", user.toClientDoc());
        res.end(true);
    };

    public Handler getAllUsers = (req, res) -> {
        ArrayList<String> users = new ArrayList<>();
        try {
            db.col("users").find().forEach((Block<Document>)
                    (doc) -> users.add(doc.getString("username")));
        } catch (DatabaseException exc) {
            res.err("There was a database error.", exc);
            return;
        }
        res.append("users", users);
        res.end(true);
    };


    public Handler refreshJwt = (req, res) -> {
        String jwt = req.getData("jwt");
        Boolean validated = null;
        try {
            validated = User.validateJwt(jwt);
        } catch (JwtException exc) {
            res.err("There was a JWT validation error", exc);
            return;
        }
        if (validated) {
            String newJwt = null;
            try {
                newJwt = req.getUser().giveJwt();
            } catch (DatabaseException exc) {
                res.err("There was a database error.", exc);
                return;
            }
            res.append("jwt", newJwt);
            res.append("user", req.getUser().serialize());
            res.end(true);
        } else {
            res.append("errorMessage", "The the old JWT did not match.");
            res.end(false);
        }
    };
}
