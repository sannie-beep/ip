package utilities;

import commands.Command;
import commands.GoodbyeCommand;
import tasks.Deadline;
import tasks.Event;
import tasks.Task;
import tasks.Todo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * The Parser class processes user input and executes the corresponding actions in the Bigmouth chatbot.
 */
public class Parser {
    private Ui ui;
    private Storage storage;
    private TaskList tasks;

    /**
     * Constructs a new Parser with the specified Ui, TaskList, and Storage.
     *
     * @param ui    The Ui that handles response strings in the commands.
     * @param tasks The task list that stores all the tasks.
     * @param s     The storage handler for saving and loading tasks.
     */
    public Parser(Ui ui, TaskList tasks, Storage s) {
        this.ui = ui;
        this.tasks = tasks;
        this.storage = s;
    }

    /**
     * Parses a date/time string and returns the corresponding LocalDateTime object.
     *
     * @param input The input string in the format 'M/d/yyyy HHmm'.
     * @return A LocalDateTime object representing the parsed date and time.
     * @throws BigmouthException If the input format is incorrect.
     */
    static LocalDateTime parseDateTime(String input) throws BigmouthException {
        try {
            return LocalDateTime.parse(input, DateTimeFormatter.ofPattern("M/d/yyyy HHmm"));
        } catch (DateTimeParseException e) {
            throw new BigmouthException("OOPS! Please enter the date/time " +
                    "in the format 'M/d/yyyy HHmm'.");
        }
    }

    /**
     * Parses the user input string and returns the corresponding Command object based on the action.
     *
     * @param userInput The input string entered by the user.
     * @return A Command object representing the user's action.
     */
    public Command parseInput(String userInput) {
        String firstWord = userInput.contains(" ") ? userInput.split(" ")[0] : userInput;
        try {
            switch (firstWord) {
            case "bye":
                return new GoodbyeCommand();
            case "list":
                return this.sendListCommand();
            case "mark":
                return this.sendMarkCommand(userInput);
            case "unmark":
                return this.sendUnmarkCommand(userInput);
            case "todo":
                return this.sendTodoCommand(userInput);
            case "deadline":
                return this.sendDeadlineCommand(userInput);
            case "event":
                return this.sendEventCommand(userInput);
            case "delete":
                return this.sendDeleteCommand(userInput);
            case "find":
                return this.sendFindCommand(userInput);
            }
        } catch (BigmouthException e) {
            return new Command(e.getMessage());
        } catch (NumberFormatException e) {
            return new Command("OOPS! Please enter a valid number.");
        }
        return new Command("Idk what ur sayin leh :/");
    }

    /**
     * Processes the "list" command, which displays the current list of tasks.
     *
     * @return A Command object that shows the list of tasks.
     * @throws BigmouthException If the task list is empty.
     */
    private Command sendListCommand() throws BigmouthException {
        if (tasks.isEmpty()) {
            throw new BigmouthException("Your task list is empty!");
        }
        String response = ui.showTaskList(this.tasks);
        return new Command(response);
    }

    /**
     * Processes the "mark" command to mark a task as done.
     *
     * @param userInput The input string containing the task number to be marked.
     * @return A Command object showing the updated task list.
     * @throws BigmouthException If the task number is invalid.
     */
    private Command sendMarkCommand(String userInput) throws BigmouthException {
        int taskNumber = Integer.parseInt(userInput.split(" ")[1]) - 1;
        if (taskNumber < 0 || taskNumber >= tasks.size()) {
            throw new BigmouthException("Invalid task number. " +
                    "Please enter a valid task number.");
        }
        tasks.get(taskNumber).markAsDone();
        storage.saveToFile();
        return new Command(ui.showTaskMarked(tasks.get(taskNumber)));
    }

    /**
     * Processes the "unmark" command to mark a task as not done.
     *
     * @param userInput The input string containing the task number to be unmarked.
     * @return A Command object showing the updated task list.
     * @throws BigmouthException If the task number is invalid.
     */
    private Command sendUnmarkCommand(String userInput) throws BigmouthException {
        int taskNumber = Integer.parseInt(userInput.split(" ")[1]) - 1;
        if (taskNumber < 0 || taskNumber >= tasks.size()) {
            throw new BigmouthException("Invalid task number. " +
                    "Please enter a valid task number.");
        }
        tasks.get(taskNumber).markAsNotDone();
        storage.saveToFile();
        return new Command(ui.showTaskUnmarked(tasks.get(taskNumber)));
    }

    /**
     * Processes the "todo" command to add a new Todo task.
     *
     * @param userInput The input string containing the description of the Todo task.
     * @return A Command object confirming the addition of the Todo task.
     * @throws BigmouthException If the description of the Todo task is empty.
     */
    private Command sendTodoCommand(String userInput) throws BigmouthException {
        String description = userInput.substring(5).trim();
        if (description.isEmpty()) {
            throw new BigmouthException("OOPS! The description of " +
                    "a todo cannot be empty.");
        }
        Todo curr = new Todo(description);
        tasks.add(curr);
        storage.saveToFile();
        return new Command(ui.showTaskAdded(curr, tasks.size()));
    }

    /**
     * Processes the "deadline" command to add a new Deadline task.
     *
     * @param userInput The input string containing the description and deadline of the task.
     * @return A Command object confirming the addition of the Deadline task.
     * @throws BigmouthException If the description or deadline is missing or invalid.
     */
    private Command sendDeadlineCommand(String userInput) throws BigmouthException {
        String[] parts = userInput.split(" /by ");
        if (parts.length < 2 || parts[1].trim().isEmpty()) {
            throw new BigmouthException("OOPS! The deadline " +
                    "command is missing a date/time.");
        }
        String description = parts[0].substring(9).trim();
        LocalDateTime by = Parser.parseDateTime(parts[1].trim());
        if (description.isEmpty()) {
            throw new BigmouthException("OOPS! The description of " +
                    "a deadline cannot be empty.");
        }
        Deadline curr = new Deadline(description, by);
        tasks.add(curr);
        storage.saveToFile();
        return new Command(ui.showTaskAdded(curr, tasks.size()));
    }

    /**
     * Processes the "event" command to add a new Event task.
     *
     * @param userInput The input string containing the description, start, and end time of the event.
     * @return A Command object confirming the addition of the Event task.
     * @throws BigmouthException If the description, start, or end time is missing or invalid.
     */
    private Command sendEventCommand(String userInput) throws BigmouthException {
        String[] parts = userInput.split(" /from | /to ");
        if (parts.length < 3 || parts[1].trim().isEmpty() || parts[2].trim().isEmpty()) {
            throw new BigmouthException("OOPS! The event command " +
                    "is missing a start or end time.");
        }
        String description = parts[0].substring(6).trim();
        LocalDateTime from = Parser.parseDateTime(parts[1].trim());
        LocalDateTime to = Parser.parseDateTime(parts[2].trim());
        if (description.isEmpty()) {
            throw new BigmouthException("OOPS! The description of " +
                    "an event cannot be empty.");
        }
        Event curr = new Event(description, from, to);
        tasks.add(curr);
        storage.saveToFile();
        return new Command(ui.showTaskAdded(curr, tasks.size()));
    }

    /**
     * Processes the "delete" command to delete a task from the list.
     *
     * @param userInput The input string containing the task number to be deleted.
     * @return A Command object confirming the deletion of the task.
     * @throws BigmouthException If the task number is invalid.
     */
    private Command sendDeleteCommand(String userInput) throws BigmouthException {
        int taskNumber = Integer.parseInt(userInput.split(" ")[1]) - 1;
        if (taskNumber < 0 || taskNumber >= tasks.size()) {
            throw new BigmouthException("Invalid task number. " +
                    "Please enter a valid task number.");
        }
        Task removedTask = tasks.remove(taskNumber);
        storage.saveToFile();
        return new Command(ui.showTaskRemoved(removedTask, tasks.size()));
    }

    /**
     * Processes the "find" command to search for tasks containing a keyword.
     *
     * @param userInput The input string containing the keyword to search for.
     * @return A Command object showing the tasks that match the search keyword.
     * @throws BigmouthException If the keyword is missing.
     */
    private Command sendFindCommand(String userInput) throws BigmouthException {
        String keyword = userInput.substring(5).trim();
        TaskList foundTasks = tasks.find(keyword);
        return new Command(ui.showMatchingTasks(foundTasks, tasks.size()));
    }
}
