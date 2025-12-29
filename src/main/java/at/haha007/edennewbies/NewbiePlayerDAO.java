package at.haha007.edennewbies;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NewbiePlayerDAO {
    Optional<NewbiePlayer> getByUUID(UUID uuid);

    void saveOrUpdate(NewbiePlayer player);

    boolean remove(UUID uuid);

    List<NewbiePlayer> listAll();
}
