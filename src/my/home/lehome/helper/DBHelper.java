package my.home.lehome.helper;

import java.util.List;

import android.content.Context;
import android.util.Log;
import de.greenrobot.dao.query.DeleteQuery;
import de.greenrobot.dao.query.QueryBuilder;
import de.greenrobot.lehome.ChatItem;
import de.greenrobot.lehome.ChatItemDao;
import de.greenrobot.lehome.DaoMaster;
import de.greenrobot.lehome.DaoMaster.OpenHelper;
import de.greenrobot.lehome.DaoSession;
import de.greenrobot.lehome.Shortcut;
import de.greenrobot.lehome.ShortcutDao;

public class DBHelper {
	private static final String TAG = DBHelper.class.getName();
	private static DaoMaster daoMaster;
	private static DaoSession daoSession;
	
	public static void initHelper(Context context) {
	    if (daoMaster == null)
	    {
	        OpenHelper helper = new DaoMaster.DevOpenHelper(context, "lehome_db", null);
	        daoMaster = new DaoMaster(helper.getWritableDatabase());
            daoSession = daoMaster.newSession();
	    }
	}

	public static DaoMaster getDaoMaster()
	{
	    if (daoMaster == null)
	    {
	    	Log.w(TAG, "initHelper must be call first.");
	    }
	    return daoMaster;
	}

	public static DaoSession getDaoSession()
	{
	    if (daoSession == null)
	    {
	    	Log.w(TAG, "initHelper must be call first.");
	    }
	    return daoSession;
	}
	
	public static void addChatItem(ChatItem entity) {
		getDaoSession().getChatItemDao().insert(entity);
	}
	
	public static List<ChatItem> getAllChatItems() {
		return getDaoSession().getChatItemDao().loadAll();
	}
	
	public static List<ChatItem> loadLatest(int limit) {
		if (limit <= 0) {
			Log.w(TAG, "loadAfter invaild limit.");
			return null;
		}
		QueryBuilder<ChatItem> queryBuilder = getDaoSession().getChatItemDao().queryBuilder();
		return queryBuilder
				.orderDesc(ChatItemDao.Properties.Id)
				.limit(limit)
				.list();
	}
	
	public static List<ChatItem> loadBefore(long id, int limit) {
		if (limit <= 0) {
			Log.w(TAG, "loadAfter invaild limit.");
			return null;
		}
		QueryBuilder<ChatItem> queryBuilder = getDaoSession().getChatItemDao().queryBuilder();
		return queryBuilder
				.where(ChatItemDao.Properties.Id.lt(id))
				.orderDesc(ChatItemDao.Properties.Id)
				.limit(limit)
				.list();
	}
	
	public static void addShortcut(Shortcut shortcut) {
		getDaoSession().getShortcutDao().insert(shortcut);
	}
	
	public static void updateShortcut(Shortcut shortcut) {
		getDaoSession().getShortcutDao().update(shortcut);
	}

	public static List<Shortcut> getAllShortcuts() {
		return getDaoSession().getShortcutDao().loadAll();
	}
	
	public static void deleteShortcut(long Id)
    {
        QueryBuilder<Shortcut> qb = getDaoSession().getShortcutDao().queryBuilder();
        DeleteQuery<Shortcut> bd = qb.where(ShortcutDao.Properties.Id.eq(Id)).buildDelete();
        bd.executeDeleteWithoutDetachingEntities();
    }
	
	public static void destory() {
		daoSession.clear();
		daoMaster = null;
		daoSession = null;
	}
}
