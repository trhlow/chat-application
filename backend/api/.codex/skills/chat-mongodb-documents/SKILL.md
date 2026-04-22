    ---
    name: chat-mongodb-documents
    description: Use for MongoDB domain modeling, repository queries, document relationships, pagination/history retrieval, data cleanup jobs, and any task touching domain or repository packages in this Spring Data MongoDB backend.
    ---

    # chat-mongodb-documents

## Scope
Use for:
- new MongoDB document fields
- repository query methods
- history retrieval / pagination
- room/message/friend/notification persistence changes
- scheduler-driven cleanup
- index-minded modeling discussions

## Relevant Packages
- `domain/*`
- `repository/*`
- `scheduler/DataCleanupScheduler.java`
- service implementations that orchestrate queries

## Working Rules
1. Think in document-access patterns first, not relational joins.
2. Model changes should be justified by query/use-case needs.
3. Preserve backward compatibility for existing documents when possible.
4. Keep repository interfaces expressive and service logic readable.
5. For history/pagination flows, define ordering and cursor semantics clearly.
6. Avoid repository method explosion; add custom query logic only when needed.
7. Consider data cleanup/TTL implications for tokens or ephemeral data.

## Review Checklist
- What are the main read paths?
- Does the model fit those reads?
- Are repository methods still understandable?
- Could null/legacy documents break the feature?
- Is pagination stable under concurrent writes?

## Good Output
When you change persistence behavior, explain:
- the document change
- the repository/query impact
- the service impact
- any migration or compatibility considerations
