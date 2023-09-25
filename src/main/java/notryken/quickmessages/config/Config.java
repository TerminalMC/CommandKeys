package notryken.quickmessages.config;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class Config
{
    private final Map<Integer,String> messageMap;

    public Config()
    {
        messageMap = new LinkedHashMap<>();
    }

    /**
     * @param key The input keycode associated with the message.
     * @return The message, or null if not found.
     */
    public String getMessage(int key)
    {
        return messageMap.get(key);
    }

    public Iterator<Integer> getKeyIter()
    {
        return messageMap.keySet().iterator();
    }

    public Iterator<String> getValueIter()
    {
        return messageMap.values().iterator();
    }

    /**
     * If there is a message with specified oldKeyCode, changes its key to
     * specified newKeyCode.
     * @param oldKey The original keycode.
     * @param newKey The new keycode.
     */
    public void setKey(int oldKey, int newKey)
    {
        String message = messageMap.get(oldKey);
        if (message != null) {
            messageMap.remove(oldKey);
            messageMap.put(newKey, message);
        }
    }

    /**
     * If there is a message with specified key, changes its content to
     * specified newMessage.
     * @param key The message key.
     * @param newMessage The new content.
     */
    public void setMessage(int key, String newMessage)
    {
        messageMap.replace(key, newMessage);
    }

    /**
     * Adds a new message with maximum key and message "", if one does not
     * already exist.
     */
    public void addMessage()
    {
        if (!messageMap.containsKey(Integer.MAX_VALUE)) {
            messageMap.put(Integer.MAX_VALUE, "");
        }
    }

    /**
     * Removes the message with specified key, if it exists.
     * @param key The input keycode of the message.
     */
    public void removeMessage(int key)
    {
        messageMap.remove(key);
    }

    public void purge()
    {
        messageMap.values().removeIf(String::isEmpty);
    }
}
