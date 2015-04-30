package com.messagebus.interactor.pubsub.impl.redis;

import com.messagebus.interactor.pubsub.IPubSubListener;
import com.messagebus.interactor.pubsub.IPubSuber;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.nio.charset.Charset;

/**
 * long live redis
 */
public class LongLiveRedis implements IPubSuber {

    private static final Log logger = LogFactory.getLog(LongLiveRedis.class);

    private Jedis          jedis;
    private JedisPool      jedisPool;
    private String         host;
    private int            port;
    private ChannelWatcher watcher;

    public LongLiveRedis() {
    }

    private void init() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(1);
        jedisPool = new JedisPool(jedisPoolConfig, this.getHost(), this.getPort());
        jedis = jedisPool.getResource();
        jedis.connect();

        try {
            jedis.ping();
        } catch (Exception e) {
            throw new RuntimeException("jedis is not connected to : "
                                           + this.getHost() + ":" + this.getPort());
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void watch(String[] channels, IPubSubListener listener) {
        if (watcher != null) {
            watcher.stop();
            watcher = null;
        }

        watcher = new ChannelWatcher();
        watcher.setListener(listener);
        watcher.setChannels(channels);

        watcher.start();
    }

    public void publish(String channel, byte[] data) {
        //because redis do not store data when publishing
        //so we should store the data and use the channel name as the key
        jedis.publish(channel.getBytes(Charset.defaultCharset()), data);
    }

    public byte[] get(String key) {
        return jedis.get(key.getBytes(Charset.defaultCharset()));
    }

    @Override
    public boolean exists(String key) {
        return jedis.exists(key);
    }

    @Override
    public void set(String key, byte[] data) {
        jedis.set(key.getBytes(Charset.defaultCharset()), data);
    }

    public void open() {
        this.init();
    }

    public void close() {
        watcher.stop();
//        jedis.close();
        if (jedisPool != null) {
            jedisPool.returnResource(jedis);
            jedisPool.destroy();
        }
    }

    public boolean isAlive() {
        return jedis.isConnected();
    }

    private class ChannelWatcher implements Runnable {

        private IPubSubListener listener;
        private String[]        channels;
        private Jedis           suber;
        private Thread          currentThread;
        private JedisPubSub     realPubSuber;

        public ChannelWatcher() {
            suber = new Jedis(getHost(), getPort());
            currentThread = new Thread(this);
        }

        @Override
        public void run() {
            try {
                realPubSuber = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        getListener().onChange(channel, message.getBytes(Charset.defaultCharset()), null);
                    }
                };

                suber.subscribe(realPubSuber, getChannels());
            } catch (Exception e) {
                logger.error(e.toString());
            } finally {
                suber.close();
            }
        }

        public void start() {
            currentThread.start();
        }

        public void stop() {
            if (realPubSuber != null) {
                realPubSuber.unsubscribe();
            }
            currentThread.interrupt();
        }

        public IPubSubListener getListener() {
            return listener;
        }

        public void setListener(IPubSubListener listener) {
            this.listener = listener;
        }

        public String[] getChannels() {
            return channels;
        }

        public void setChannels(String[] channels) {
            this.channels = channels;
        }
    }


}
