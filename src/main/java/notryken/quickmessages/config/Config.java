package notryken.quickmessages.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Config
{
    private final ArrayList<String[]> keyMessages;

    public Config()
    {
        keyMessages = new ArrayList<>();
    }

    public Config(ArrayList<String[]> keyMessages)
    {
        this.keyMessages = keyMessages;
    }

    /**
     * Returns the message paired with the specified key, or null if not found.
     * @param key The message key.
     * @return The corresponding message, or null if not found.
     */
    public String getMessage(String key)
    {
        for (String[] message : keyMessages) {
            if (message[0].equals(key)) {
                return message[1];
            }
        }
        return null;
    }

    public String[] getKeyMessage(String key)
    {
        for (String[] message : keyMessages) {
            if (message[0].equals(key)) {
                return message;
            }
        }
        return null;
    }

    /**
     * @return An unmodifiable view of the message list.
     */
    public List<String[]> getKeyMessages()
    {
        return Collections.unmodifiableList(keyMessages);
    }

    public int getNumMessages()
    {
        return keyMessages.size();
    }

    /**
     * If there is a message with specified oldKey, changes its key to
     * specified newKey.
     * @param oldKey The original key.
     * @param newKey The new key.
     * @return 0 if successful, -1 otherwise.
     */
    public int setKey(String oldKey, String newKey)
    {
        String[] keyMessage = getKeyMessage(oldKey);
        if (keyMessage != null) {
            keyMessage[0] = newKey;
            return 0;
        }
        return -1;
    }

    /**
     * If there is a message with specified key, changes its content to
     * specified newMessage.
     * @param key The message key.
     * @param newMessage The new content.
     * @return 0 if successful, -1 otherwise.
     */
    public int setMessage(String key, String newMessage)
    {
        String[] keyMessage = getKeyMessage(key);
        if (keyMessage != null) {
            keyMessage[1] = newMessage.strip();
            return 0;
        }
        return -1;
    }

    /**
     * Adds a new message with key "" and message "" to the list.
     */
    public void addMessage()
    {
        if (getMessage("") == null) {
            keyMessages.add(new String[]{"", ""});
        }
    }

    public void removeMessage(int index)
    {
        if (index >= 0 && index < keyMessages.size()) {
            keyMessages.remove(index);
        }
    }

    public void purge()
    {
        keyMessages.removeIf(keyMessage -> keyMessage[1].equals(""));
    }
}
