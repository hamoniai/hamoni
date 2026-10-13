= Hamoni version 2

== General architecture

Design considerations

1. Still needs HA & scaling for larger deployments.
2. Control room. Seperated or merged. Merged makes sense from cost/simplicity, but limits flexibility.
  - seperated makes sense for regulated companies etc. Can also deploy on many more platform types.
  - Use Qarkus + Angular to create a light frontend.
3. Backend runners. Ideally these should be quarkus images. However the image may exist on multiple platforms.
  - Should we seperate an RPA runner from a business runner - I think so.
4. Security. Quarkus on the frontend could link directly to a client SSO, which migigates all of the issues with the keycloak standup and syncing.
  - How to authenticate to the backend. Assuming we are on a cluster, we could use a token? or better still, signed messages. That way there's no limit
    of expiry on the message.
5. Processing.
  - If we assume that the frontend handles all APIs, then we need to be aware of what the backend is doing. So a shared database/state store. Or we need
    to query the process engine via message each time. The latter is simpler from a sharing perspective, but also means there will be a wait time on the
    front
  - All interaction with the outside world should happen through the frontend.
5. Comms. Between front and back. A 1-1 conenction such as a web socket isn't great, the frontend had to hold and track multiple connections. A ping channel
   would be a better idea.
  - Sitting on AWS we can utilise SQS.
  - Can have a few channels,
   - A ping channel for registration/alive checking
   - A broadcast channel for first to answer a message
   - a point to point channel for each executor that allows directed communication
   - Lets assume we need to use a message queue between. How to configure for local testing?
6. Logging and auditing.
7. Process design and testing.
   - Assume process design and testing all happens locally. That is a lot simpler in terms of build as there is no maven etc.
   - We upload a package, which is a package. The BPMN diagrams have already been compiled and the image nodes etc created. We still have the BPMN for diagram display and tracking.
   - We upload versions of the package. How to deal with extensions. 
   - Extensions/integrations should be cleanly compiled into seperate packages. What about common dependencies. We don't want to be pulling random packages really, it's a security issue.
   - All code gets run on the backend. The frontend only tracks what code is running.
   - What to do about screen design. Maybe there is some open library for this.
8. Startup/config
  - Try to get rid of the init.
  - All flags should be set at startup, i.e. the loging location etc.


Updates: The platform is primarrily a businesss focussed, not developer focussed platform. Therefore the process editor should be online to allow quick changes.

- That implies that the data editor should also be online.
- Let's assume that simple business rules can be edited in the project. not as seperate assets, but all entrance is through the workflow builder.
- Have a simple & advanced mode. 
  - Simple mode: basic flow chart syntax.
  - Data is just properties with some form of auto-typing.
  - Advanced: switches to BPMN full syntax. For data objects, it has an embedded editor. SHould classes be Java or Kotlin?
  - If we download the package, it gets a standard layout with the BPMN (non compiled, data classes and rules).
  - Standard simple layout, and custom builder, not linking Maven in.
  - Each project change is auto-versioned.
   - What about locking, what happens to newer versions on rollback. Do we use Git on a filesystem? Might be overkil.
   - Need to think about a simple versioning strategy.
  - For packaging, how do we deliver to the executor?

- Considerations
 - Process designer
 - AAA and initial account.
 - Credential store
 - Document store
 - Templates (related to document templating)
 - Relese manager
 - Dashboard and overviews
 - Data interfaces/APIs
 - Log download and auditing (maybe as a part of the data interface?)


- Work screen design:
 - have native ability to do A/B testing (for any cmoponent or the whole screen?)
 - Have an AI assistant box on the screen that can be asked to do work. Not just reference docs, but provide updated/new docs
   - How to update the case data?
 - Mobile/desktop variants or responsive?

- Workflow
 - need to be able to start manually.
    - Should have a bulk option.
    - Can download the process data for a time period through the UI - work logs
 - work assignment could be rule based (capabilities), manual or virtual manager
  - Do we really want manual. It would leave all work unallocated until someone assigns
  - Capabiltieis means we need to define those capabilities on the staff. What if no capabilities match
 - Virtual manager - one for each "team", a task is allocated by the virtual manager, which has a pool of resources (may be AI?).
  - Certain conditions predict certain people - complexity etc.
  - When getting the next work, it looks at all the work a person can do... or remembering that a person could be part of multiple "teams", does it assign work when entering
    a manual step.
   - Need to track assigned, started, completed. (may have multiple suspend/resume), and an outcome that is refer.
  - Process designer needs "stages", how to manage these. Could do steps in the flow, but this is cumersome and nuanced. Better to mark/segment?
   - COuld assign metadata to each step? but then what happens if people copy/paste and don't update.
  - Work assignment needs thought. Assign on creation, or on get next?
  - Data types: Have basic types - string, boolean etc, but also table and json (or dict). Json is ab abstraction of dict?
    - also need Document. Each item should be individual or a list. Or Object
    - to get other data types, need to swith to advanced mode, then there's a type-ahead
  - do we have the concept of a business area or business line ? Is this built into the folder structure or more of a tag?
    - a tag system might be heirarchal?
 - Staff atributes
  - No login/user if using SSO. If using SSO, then block all local accounts. However, might have a user/capabilities export/import
  - Available - if the staff is available today or not. Could have a non-available reason, as part of the user side setup. Might have some defaults though
    e.g. "holiday", "sick" etc. Could be customisable, Allow do remove if already used? How to map
  - Capabilties (experience)
   - These would map to a rule engine.
 - Process attributes
  - Set and overall SLA.
 - Task attributes
  - Might set and initial work time, but this is dynamically assigned.
  - Do we need task level max wait-time. Competes somewhat with the overall process SLA.

- Trial/demo mode
 - No integrations. Well, this shouldn:t be the case, but how to isolate the credential store for multi-tennant vs single tennant control rooms without giving away the platform.
 - In essence the control room should be single-tennant. However, how to handle the demo mode?

Commercial approach - better to get people on and hooked, otherwise it's an uphill struggle

Principle - 80% should be easy, but the remaining 20% should not be hard. Just need to switch to advanced mode to get full control
 - Should be ops/user focussed and easy for them.

How to handle multi-tennants. SHould have an AI connector for staff ideally, but this means a multi-tennant approach.
 - How to limit others from picking up the same tech?
 - How to restrict the usefulness. Number of processes, number of tasks, number of staff, number of hours worked?
  - Need to restrict but not too aggressively. Still needs to be useful, but puts people in a position where they want more.
  - COuld be unlimited people processes, but then restruicted AI etc?
  - AI should use major vendors, Claude etc directly
  - RPA should be offlimits - i.e. it needs specific setup for working with, queues etc, witch likely only system admins have?
  - For multi-tennant version, AI designer is there,
    - "I wnat a process that had three tasks, all in one team, The first task shooul dbe done by....
    - Data between tasks is optional, since it could be just manual processing
    - 
  - For multi-tennany, can rate limit/quota external API, no internal API though.
   - How best to rate limite/quota. Everyone has the concept of usage limits now, but can that be used?
   - Better to rate limit processes? This does make things awkward when there are KPIs etc.

Outcomes:
 * Have the concept of tennant built in. This is part of the provisioning. Build it into the data model etc. The idea is that businesses with multiple
   different BUs could have the same tennant or different. The question then becomes, how to provision the first tennant? Maybe that is a startup parameter?
   or it scans the database for the tennats and initialises (bad idea, each node would do this). Tennant setup needs to be explicit. Maybe have a platform
   account that acts as system? Provisions based on an API that is not exposed? What about clients that want to use the multi-tennant local? How would they
   provision? A platform account that has provisioning access might be good, but how to setup/configure it

Architecture:
 * messages do evertyhing - start, stop, alerts etc.
   * Use JMS for the most, Kafka is for user side consumption of messages
 * Layers:
   * platform_manager -> can setup the environments, add/remove runners etc. Dedicated role. SHould not see the rest of the platform. Might have a local login
   * team_manager -> focusses on work they do. Need some way to identify they processes they look after (or subsections of the process)
   * process_manager -> focusses on the overall metrics and performance of the process(es) they manage
   * business_line_lead -> focussses on all processes in a business line.
   * staff -> focusses on the work to get done.
