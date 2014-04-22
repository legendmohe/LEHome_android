package de.greenrobot.lehome;

import android.database.sqlite.SQLiteDatabase;

import java.util.Map;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.AbstractDaoSession;
import de.greenrobot.dao.identityscope.IdentityScopeType;
import de.greenrobot.dao.internal.DaoConfig;
import de.greenrobot.lehome.ChatItem;
import de.greenrobot.lehome.ChatItemDao;
import de.greenrobot.lehome.Shortcut;
import de.greenrobot.lehome.ShortcutDao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see de.greenrobot.dao.AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig chatItemDaoConfig;
    private final DaoConfig shortcutDaoConfig;

    private final ChatItemDao chatItemDao;
    private final ShortcutDao shortcutDao;

    public DaoSession(SQLiteDatabase db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        chatItemDaoConfig = daoConfigMap.get(ChatItemDao.class).clone();
        chatItemDaoConfig.initIdentityScope(type);

        shortcutDaoConfig = daoConfigMap.get(ShortcutDao.class).clone();
        shortcutDaoConfig.initIdentityScope(type);

        chatItemDao = new ChatItemDao(chatItemDaoConfig, this);
        shortcutDao = new ShortcutDao(shortcutDaoConfig, this);

        registerDao(ChatItem.class, chatItemDao);
        registerDao(Shortcut.class, shortcutDao);
    }
    
    public void clear() {
        chatItemDaoConfig.getIdentityScope().clear();
        shortcutDaoConfig.getIdentityScope().clear();
    }

    public ChatItemDao getChatItemDao() {
        return chatItemDao;
    }

    public ShortcutDao getShortcutDao() {
        return shortcutDao;
    }

}
