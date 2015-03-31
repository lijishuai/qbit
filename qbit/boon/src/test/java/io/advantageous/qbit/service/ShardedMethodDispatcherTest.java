/*******************************************************************************

 * Copyright (c) 2015. Rick Hightower, Geoff Chandler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  ________ __________.______________
 *  \_____  \\______   \   \__    ___/
 *   /  / \  \|    |  _/   | |    |  ______
 *  /   \_/.  \    |   \   | |    | /_____/
 *  \_____\ \_/______  /___| |____|
 *         \__>      \/
 *  ___________.__                  ____.                        _____  .__                                             .__
 *  \__    ___/|  |__   ____       |    |____ ___  _______      /     \ |__| ___________  ____  ______ ______________  _|__| ____  ____
 *    |    |   |  |  \_/ __ \      |    \__  \\  \/ /\__  \    /  \ /  \|  |/ ___\_  __ \/  _ \/  ___// __ \_  __ \  \/ /  |/ ___\/ __ \
 *    |    |   |   Y  \  ___/  /\__|    |/ __ \\   /  / __ \_ /    Y    \  \  \___|  | \(  <_> )___ \\  ___/|  | \/\   /|  \  \__\  ___/
 *    |____|   |___|  /\___  > \________(____  /\_/  (____  / \____|__  /__|\___  >__|   \____/____  >\___  >__|    \_/ |__|\___  >___  >
 *                  \/     \/                \/           \/          \/        \/                 \/     \/                    \/    \/
 *  .____    ._____.
 *  |    |   |__\_ |__
 *  |    |   |  || __ \
 *  |    |___|  || \_\ \
 *  |_______ \__||___  /
 *          \/       \/
 *       ____. _________________    _______         __      __      ___.     _________              __           __      _____________________ ____________________
 *      |    |/   _____/\_____  \   \      \       /  \    /  \ ____\_ |__  /   _____/ ____   ____ |  | __ _____/  |_    \______   \_   _____//   _____/\__    ___/
 *      |    |\_____  \  /   |   \  /   |   \      \   \/\/   // __ \| __ \ \_____  \ /  _ \_/ ___\|  |/ // __ \   __\    |       _/|    __)_ \_____  \   |    |
 *  /\__|    |/        \/    |    \/    |    \      \        /\  ___/| \_\ \/        (  <_> )  \___|    <\  ___/|  |      |    |   \|        \/        \  |    |
 *  \________/_______  /\_______  /\____|__  / /\    \__/\  /  \___  >___  /_______  /\____/ \___  >__|_ \\___  >__| /\   |____|_  /_______  /_______  /  |____|
 *                   \/         \/         \/  )/         \/       \/    \/        \/            \/     \/    \/     )/          \/        \/        \/
 *  __________           __  .__              __      __      ___.
 *  \______   \ ____   _/  |_|  |__   ____   /  \    /  \ ____\_ |__
 *  |    |  _// __ \  \   __\  |  \_/ __ \  \   \/\/   // __ \| __ \
 *   |    |   \  ___/   |  | |   Y  \  ___/   \        /\  ___/| \_\ \
 *   |______  /\___  >  |__| |___|  /\___  >   \__/\  /  \___  >___  /
 *          \/     \/             \/     \/         \/       \/    \/
 *
 * QBit - The Microservice lib for Java : JSON, WebSocket, REST. Be The Web!
 *  http://rick-hightower.blogspot.com/2014/12/rise-of-machines-writing-high-speed.html
 *  http://rick-hightower.blogspot.com/2014/12/quick-guide-to-programming-services-in.html
 *  http://rick-hightower.blogspot.com/2015/01/quick-start-qbit-programming.html
 *  http://rick-hightower.blogspot.com/2015/01/high-speed-soa.html
 *  http://rick-hightower.blogspot.com/2015/02/qbit-event-bus.html

 ******************************************************************************/

package io.advantageous.qbit.service;

import io.advantageous.qbit.client.ClientProxy;
import io.advantageous.qbit.queue.QueueBuilder;
import io.advantageous.qbit.service.dispatchers.ServiceWorkers;
import io.advantageous.qbit.test.TimedTesting;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static io.advantageous.boon.core.Exceptions.die;
import static io.advantageous.qbit.queue.QueueBuilder.queueBuilder;
import static io.advantageous.qbit.service.ServiceBuilder.serviceBuilder;
import static io.advantageous.qbit.service.ServiceBundleBuilder.serviceBundleBuilder;
import static io.advantageous.qbit.service.dispatchers.ServiceWorkers.shardedWorkers;

/**
 * @author rhightower
 *         on 2/19/15.
 */
public class ShardedMethodDispatcherTest extends TimedTesting {


    ServiceBundle bundle;

    ServiceWorkers dispatcher;
    boolean ok = true;

    @Before
    public void setup() {

        super.setupLatch();
        QueueBuilder queueBuilder = queueBuilder().setBatchSize(1001);

        int workerCount = 10;//Runtime.getRuntime().availableProcessors();

//        dispatcher = workers();


        dispatcher = shardedWorkers((methodName, methodArgs, numWorkers) -> {
            String userName = methodArgs[0].toString();
            return userName.hashCode() % numWorkers;
        });


        final ServiceBuilder serviceBuilder = serviceBuilder()
                .setRequestQueueBuilder(queueBuilder).setResponseQueueBuilder(queueBuilder);

        for (int index = 0; index < workerCount; index++) {
            final ServiceQueue serviceQueue = serviceBuilder
                    .setServiceObject(new ContentRulesEngine()).build();
            dispatcher.addServices(serviceQueue);

        }

        dispatcher.start();

        bundle = serviceBundleBuilder().setAddress("/root").build();

        //bundle.addServiceObject("/workers", new ContentRulesEngine());
        bundle.addServiceConsumer("/workers", dispatcher);

        bundle.start();

    }

    @After
    public void tearDown() {
        bundle.stop();

    }

    @Test
    public void test() {

        final MultiWorkerClient worker = bundle.createLocalProxy(MultiWorkerClient.class, "/workers");

        for (int index = 0; index < 20_000; index++) {
            worker.pickSuggestions("rickhigh" + index);
        }

        worker.clientProxyFlush();


        super.waitForTrigger(5, o -> ContentRulesEngine.totalCount.get() >= 20_000);

        ok = ContentRulesEngine.totalCount.get() == 20_000 || die(ContentRulesEngine.totalCount);


    }


    public static interface MultiWorkerClient extends ClientProxy {
        void pickSuggestions(String username);
    }

    public static class ContentRulesEngine {

        static AtomicInteger totalCount = new AtomicInteger();

        int count;

        void pickSuggestions(String username) {
            count++;
            totalCount.incrementAndGet();
        }

    }


}